package com.flipkart.krystal.lattice.ext.a2a.codegen;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants.IMMUT_REQUEST_POJO_SUFFIX;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.flipkart.krystal.lattice.ext.a2a.A2AServerDopant;
import com.flipkart.krystal.lattice.ext.a2a.api.A2AServer;
import com.flipkart.krystal.lattice.ext.a2a.api.AgentSkill;
import com.flipkart.krystal.vajram.codegen.common.models.FacetGenModel;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import lombok.extern.slf4j.Slf4j;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.TextPart;

/**
 * Generates a Quarkus CDI bean that implements {@link AgentExecutor} by routing A2A task execution
 * and cancellation requests to the Vajrams declared in the {@link A2AServer} annotation.
 *
 * <p>For each Lattice application annotated with {@link A2AServer} this provider generates a single
 * class named {@code $AppName$_QuarkusA2AAgentExecutor} that:
 *
 * <ul>
 *   <li>implements {@link AgentExecutor}
 *   <li>is a {@link Singleton} CDI bean (automatically overrides the {@code @DefaultBean} provided
 *       by the A2A Java SDK)
 *   <li>routes {@code execute} calls to the configured executor Vajrams
 *   <li>routes {@code cancel} calls to the configured canceller Vajrams (or falls back to an
 *       immediate {@code emitter.cancel()} if no canceller is declared)
 * </ul>
 *
 * <p>Input facets of the executor / canceller Vajrams are populated from the {@link RequestContext}
 * using name conventions documented in {@link AgentSkill}.
 */
@Slf4j
@AutoService(LatticeCodeGeneratorProvider.class)
public class QuarkusA2ACodegenProvider implements LatticeCodeGeneratorProvider {

  private static final String REQUEST_CONTEXT_PARAM = "requestContext";
  private static final String EMITTER_PARAM = "emitter";

  @Override
  public CodeGenerator create(LatticeCodegenContext latticeCodegenContext) {
    return () -> {
      TypeElement latticeAppTypeElement = latticeCodegenContext.latticeAppTypeElement();
      A2AServer a2aServer = latticeAppTypeElement.getAnnotation(A2AServer.class);
      if (a2aServer == null) {
        log.warn(
            "A2AServer annotation is missing on lattice app {}. Skipping {}",
            latticeAppTypeElement,
            QuarkusA2ACodegenProvider.class);
        return;
      }
      if (latticeCodegenContext.codegenPhase() != CodegenPhase.FINAL) {
        return;
      }

      CodeGenUtility util = latticeCodegenContext.codeGenUtility().codegenUtil();
      generateAgentExecutor(latticeCodegenContext, util, latticeAppTypeElement, a2aServer);
    };
  }

  private void generateAgentExecutor(
      LatticeCodegenContext ctx,
      CodeGenUtility util,
      TypeElement latticeAppTypeElement,
      A2AServer a2aServer) {

    AgentSkill[] agentSkills = a2aServer.agent().skills();
    if (agentSkills.length == 0) {
      util.error("@A2AServer must declare at least one @Agent", latticeAppTypeElement);
      return;
    }

    String packageName = util.getPackageName(latticeAppTypeElement);
    String className = latticeAppTypeElement.getSimpleName() + "_QuarkusA2AAgentExecutor";
    ClassName executorClassName = ClassName.get(packageName, className);

    TypeSpec.Builder classBuilder =
        util.classBuilder(className, latticeAppTypeElement.getQualifiedName().toString())
            .addModifiers(PUBLIC)
            .addAnnotation(Singleton.class)
            .addSuperinterface(AgentExecutor.class);

    // Add the dopant field and constructor
    classBuilder.addField(A2AServerDopant.class, "a2aServerDopant", PRIVATE, FINAL);
    classBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addAnnotation(Inject.class)
            .addParameter(A2AServerDopant.class, "a2aServerDopant")
            .addStatement("this.a2aServerDopant = a2aServerDopant")
            .build());

    // Build execute() method
    classBuilder.addMethod(buildExecuteMethod(ctx, util, agentSkills));

    // Build cancel() method
    classBuilder.addMethod(buildCancelMethod(ctx, util, agentSkills));

    util.generateSourceFile(
        executorClassName.canonicalName(),
        JavaFile.builder(packageName, classBuilder.build()).build(),
        latticeAppTypeElement);
  }

  private MethodSpec buildExecuteMethod(
      LatticeCodegenContext ctx, CodeGenUtility util, AgentSkill[] agentSkills) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("execute")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(RequestContext.class, REQUEST_CONTEXT_PARAM)
            .addParameter(AgentEmitter.class, EMITTER_PARAM)
            .addException(A2AError.class);

    // Acknowledge immediately so the task is queued; the vajram completes it asynchronously
    method.addStatement("$L.submit()", EMITTER_PARAM);

    if (agentSkills.length == 1) {
      method.addCode(buildVajramExecutionBlock(ctx, util, agentSkills[0]));
    } else {
      // Route by skillId in request metadata
      method.addStatement(
          "$T $L = $L.getMetadata() != null ? $L.getMetadata().get($S) : null",
          Object.class,
          "skillIdObj",
          REQUEST_CONTEXT_PARAM,
          REQUEST_CONTEXT_PARAM,
          "skillId");
      method.addStatement(
          "String $L = ($L instanceof String s) ? s : $S",
          "skillId",
          "skillIdObj",
          agentSkills[0].name());

      CodeBlock.Builder switchBlock = CodeBlock.builder().beginControlFlow("switch (skillId)");
      for (AgentSkill agentSkill : agentSkills) {
        switchBlock.add("case $S:\n", agentSkill.name());
        switchBlock.indent();
        switchBlock.add(buildVajramExecutionBlock(ctx, util, agentSkill));
        switchBlock.addStatement("break");
        switchBlock.unindent();
      }
      switchBlock.add("default:\n");
      switchBlock.indent();
      switchBlock.addStatement(
          "$L.fail($L.newAgentMessage($T.of(new $T(\"Unknown agent skill: \" + skillId)), $T.of()))",
          EMITTER_PARAM,
          EMITTER_PARAM,
          List.class,
          TextPart.class,
          Map.class);
      switchBlock.unindent();
      switchBlock.endControlFlow();
      method.addCode(switchBlock.build());
    }

    return method.build();
  }

  private MethodSpec buildCancelMethod(
      LatticeCodegenContext ctx, CodeGenUtility util, AgentSkill[] agentSkills) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("cancel")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(RequestContext.class, REQUEST_CONTEXT_PARAM)
            .addParameter(AgentEmitter.class, EMITTER_PARAM)
            .addException(A2AError.class);

    if (agentSkills.length == 1) {
      method.addCode(buildCancelBlock(ctx, util, agentSkills[0]));
    } else {
      method.addStatement(
          "$T $L = $L.getMetadata() != null ? $L.getMetadata().get($S) : null",
          Object.class,
          "skillIdObj",
          REQUEST_CONTEXT_PARAM,
          REQUEST_CONTEXT_PARAM,
          "skillId");
      method.addStatement(
          "String $L = ($L instanceof String s) ? s : $S",
          "skillId",
          "skillIdObj",
          agentSkills[0].name());

      CodeBlock.Builder switchBlock = CodeBlock.builder().beginControlFlow("switch (skillId)");
      for (AgentSkill agentSkill : agentSkills) {
        switchBlock.add("case $S:\n", agentSkill.name());
        switchBlock.indent();
        switchBlock.add(buildCancelBlock(ctx, util, agentSkill));
        switchBlock.addStatement("break");
        switchBlock.unindent();
      }
      switchBlock.add("default:\n");
      switchBlock.indent();
      switchBlock.addStatement("$L.cancel()", EMITTER_PARAM);
      switchBlock.unindent();
      switchBlock.endControlFlow();
      method.addCode(switchBlock.build());
    }

    return method.build();
  }

  /**
   * Extracts the {@link TypeMirror} from a single-class annotation member (not an array). Must use
   * the {@link MirroredTypeException} trick because accessing a Class-typed annotation member at
   * annotation-processor time always throws.
   */
  @SuppressWarnings("ReturnValueIgnored")
  private static TypeMirror getTypeFromAnnotationMember(Supplier<Class<?>> supplier) {
    try {
      supplier.get();
      throw new AssertionError("Expected MirroredTypeException from annotation member access");
    } catch (MirroredTypeException mte) {
      return mte.getTypeMirror();
    }
  }

  private CodeBlock buildVajramExecutionBlock(
      LatticeCodegenContext ctx, CodeGenUtility util, AgentSkill agentSkill) {
    TypeMirror executorType = getTypeFromAnnotationMember(agentSkill::executor);
    VajramInfo vajramInfo = ctx.codeGenUtility().computeVajramInfo(executorType);
    ClassName requestClass =
        ClassName.get(
            vajramInfo.lite().packageName(), vajramInfo.vajramName() + IMMUT_REQUEST_POJO_SUFFIX);

    CodeBlock requestBuilder = buildRequestFromContext(vajramInfo, requestClass, false);

    return CodeBlock.builder()
        .add(
            """
            a2aServerDopant.executeAgent($L)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        $L.fail($L.newAgentMessage(
                            $T.of(new $T(error.getMessage() != null ? error.getMessage() : "Execution failed")),
                            $T.of()));
                    } else {
                        $L.complete($L.newAgentMessage(
                            $T.of(new $T(result != null ? result.toString() : "")),
                            $T.of()));
                    }
                });
            """,
            requestBuilder,
            EMITTER_PARAM,
            EMITTER_PARAM,
            List.class,
            TextPart.class,
            Map.class,
            EMITTER_PARAM,
            EMITTER_PARAM,
            List.class,
            TextPart.class,
            Map.class)
        .build();
  }

  private CodeBlock buildCancelBlock(
      LatticeCodegenContext ctx, CodeGenUtility util, AgentSkill agentSkill) {
    List<? extends TypeMirror> cancellerTypes =
        util.getTypesFromAnnotationMember(agentSkill::canceller).stream().toList();

    if (cancellerTypes.isEmpty()) {
      // No canceller declared – immediately acknowledge cancellation
      return CodeBlock.builder().addStatement("$L.cancel()", EMITTER_PARAM).build();
    }

    VajramInfo vajramInfo = ctx.codeGenUtility().computeVajramInfo(cancellerTypes.get(0));
    ClassName requestClass =
        ClassName.get(
            vajramInfo.lite().packageName(), vajramInfo.vajramName() + IMMUT_REQUEST_POJO_SUFFIX);

    CodeBlock requestBuilder = buildRequestFromContext(vajramInfo, requestClass, true);

    return CodeBlock.builder()
        .add(
            """
            a2aServerDopant.cancelAgent($L)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        $L.fail();
                    } else {
                        $L.cancel();
                    }
                });
            """,
            requestBuilder,
            EMITTER_PARAM,
            EMITTER_PARAM)
        .build();
  }

  /**
   * Builds the {@code ImmutableRequest._builder()...._build()} code block by mapping each input
   * facet from the incoming {@link RequestContext} using name conventions.
   *
   * <ul>
   *   <li>{@code userInput} / {@code input} / {@code message} / {@code text} → {@code
   *       requestContext.getUserInput()}
   *   <li>{@code taskId} → {@code requestContext.getTaskId()}
   *   <li>{@code contextId} → {@code requestContext.getContextId()}
   * </ul>
   */
  private CodeBlock buildRequestFromContext(
      VajramInfo vajramInfo, ClassName requestClass, boolean isCanceller) {
    CodeBlock.Builder builder = CodeBlock.builder().add("$T._builder()", requestClass);

    for (FacetGenModel facet : vajramInfo.facetStream().toList()) {
      if (facet.facetType() != FacetType.INPUT) {
        continue;
      }
      String facetName = facet.name();
      String contextExpr = resolveContextExpression(facetName, isCanceller);
      if (contextExpr != null) {
        builder.add(".$L($L.$L)", facetName, REQUEST_CONTEXT_PARAM, contextExpr);
      }
    }

    builder.add("._build()");
    return builder.build();
  }

  private static String resolveContextExpression(String facetName, boolean isCanceller) {
    return switch (facetName) {
      case "userInput", "input", "message", "text" -> isCanceller ? null : "getUserInput()";
      case "taskId" -> "getTaskId()";
      case "contextId" -> "getContextId()";
      default -> null;
    };
  }
}
