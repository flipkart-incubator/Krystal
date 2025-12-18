package com.flipkart.krystal.lattice.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.lattice.codegen.spi.di.Binding;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsProvider;
import com.flipkart.krystal.lattice.codegen.spi.di.ProviderMethod;
import com.flipkart.krystal.lattice.core.LatticeAppBootstrap;
import com.flipkart.krystal.lattice.core.LatticeAppConfig;
import com.flipkart.krystal.lattice.core.LatticeDopantSet;
import com.flipkart.krystal.lattice.core.di.Produces;
import com.flipkart.krystal.lattice.core.di.Produces.NoScope;
import com.flipkart.krystal.lattice.core.doping.AutoConfigure;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.DopeWith;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import jakarta.enterprise.context.NormalScope;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(BindingsProvider.class)
public class DopantBindingsProvider implements BindingsProvider {

  @Override
  public ImmutableList<BindingsContainer> bindings(LatticeCodegenContext context) {
    List<ExecutableElement> dopingMethods =
        context.latticeAppTypeElement().getEnclosedElements().stream()
            .filter(e -> e instanceof ExecutableElement)
            .map(ExecutableElement.class::cast)
            .filter(e -> e.getAnnotation(DopeWith.class) != null)
            .toList();
    for (ExecutableElement dopingMethod : dopingMethods) {
      validateDopingMethod(dopingMethod, context);
    }

    Map<TypeElement, DopantTypesInfo> infosBySpecBuilder =
        // Order is important. Specs have to be configured in the same order as declared in the
        // application class, so that the last dopant has the final say in how the configured dopant
        // behaves.
        new LinkedHashMap<>();
    for (ExecutableElement dopingMethod : dopingMethods) {
      ProcessingEnvironment processingEnv = context.codeGenUtility().codegenUtil().processingEnv();
      TypeMirror dopantSpecBuilderType = dopingMethod.getReturnType();
      TypeElement dopantSpecBuilderElem =
          (TypeElement) processingEnv.getTypeUtils().asElement(dopantSpecBuilderType);
      ImmutableList<TypeMirror> typeParamTypes =
          context
              .codeGenUtility()
              .codegenUtil()
              .getTypeParamTypes(
                  dopantSpecBuilderElem,
                  processingEnv
                      .getElementUtils()
                      .getTypeElement(requireNonNull(DopantSpecBuilder.class.getCanonicalName())));
      TypeMirror dopantAnnoType = typeParamTypes.get(0);
      TypeElement dopantAnnoElement =
          (TypeElement) processingEnv.getTypeUtils().asElement(dopantAnnoType);
      TypeMirror dopantConfigType = typeParamTypes.get(1);
      TypeElement dopantConfigElement =
          (TypeElement) processingEnv.getTypeUtils().asElement(dopantConfigType);
      TypeMirror dopantSpecType = typeParamTypes.get(2);
      TypeElement dopantSpecElement =
          (TypeElement) processingEnv.getTypeUtils().asElement(dopantSpecType);

      TypeMirror dopantTypeMirror =
          context
              .codeGenUtility()
              .codegenUtil()
              .getTypeParamTypes(
                  dopantSpecElement,
                  processingEnv
                      .getElementUtils()
                      .getTypeElement(requireNonNull(DopantSpec.class.getCanonicalName())))
              .get(2);
      TypeElement dopantElement =
          (TypeElement) processingEnv.getTypeUtils().asElement(dopantTypeMirror);
      DopantType dopantType = dopantElement.getAnnotation(DopantType.class);
      if (dopantType == null) {
        context
            .codeGenUtility()
            .codegenUtil()
            .error(
                "DopantType annotation is missing on " + dopantElement.getQualifiedName(),
                dopantElement);
        return ImmutableList.of();
      }

      infosBySpecBuilder.put(
          dopantSpecBuilderElem,
          new DopantTypesInfo(
              dopantType.value(),
              dopantTypeMirror,
              dopantElement,
              dopantSpecBuilderType,
              dopantSpecBuilderElem,
              dopantSpecType,
              dopantSpecElement,
              dopantConfigType,
              dopantConfigElement,
              dopantAnnoType,
              dopantAnnoElement,
              dopingMethod,
              new ArrayList<>()));
    }
    for (DopantTypesInfo dopantTypesInfo : infosBySpecBuilder.values()) {
      ProcessingEnvironment processingEnv = context.codeGenUtility().codegenUtil().processingEnv();
      TypeElement dopantSpecTypeElement = dopantTypesInfo.dopantSpecElement();
      dopantSpecTypeElement.getEnclosedElements().stream()
          .filter(e -> e instanceof ExecutableElement)
          .map(ExecutableElement.class::cast)
          .forEach(
              method -> {
                List<? extends VariableElement> autoConfigurables =
                    method.getParameters().stream()
                        .filter(variable -> variable.getAnnotation(AutoConfigure.class) != null)
                        .toList();
                if (autoConfigurables.isEmpty()) {
                  return;
                }
                if (autoConfigurables.size() != 1) {
                  context
                      .codeGenUtility()
                      .codegenUtil()
                      .error(
                          "Auto configure method must have exactly one @AutoConfigure parameter",
                          method);
                }
                TypeMirror autoConfigurableType = autoConfigurables.get(0).asType();
                Element element = processingEnv.getTypeUtils().asElement(autoConfigurableType);
                if (!context
                    .codeGenUtility()
                    .codegenUtil()
                    .isRawAssignable(autoConfigurableType, DopantSpecBuilder.class)) {
                  context
                      .codeGenUtility()
                      .codegenUtil()
                      .error("@AutoConfigure parameter must be a DopantSpecBuilder", element);
                }
                if (!(element instanceof TypeElement typeElement)) {
                  context
                      .codeGenUtility()
                      .codegenUtil()
                      .error("Auto configure parameter must be a type", element);
                  return;
                }
                DopantTypesInfo configurableTypeInfo = infosBySpecBuilder.get(typeElement);
                if (configurableTypeInfo == null) {
                  context
                      .codeGenUtility()
                      .codegenUtil()
                      .error(
                          "Could not find dopant " + typeElement.getQualifiedName(), typeElement);
                  return;
                }
                configurableTypeInfo.autoConfigurers().add(method);
              });
    }

    List<Binding> dopantSpecBindings = dopantSpecBindings(context, infosBySpecBuilder.values());
    List<Binding> dopantConfigBindings = dopantConfigBindings(context, infosBySpecBuilder.values());
    List<Binding> dopantAnnoBindings = dopantAnnoBindings(context, infosBySpecBuilder.values());
    Binding appBootstrapBinding = appBootstrapBinding(infosBySpecBuilder.values());
    Binding dopantSetBinding = dopantSetBinding(infosBySpecBuilder.values());
    List<Binding> dopantProducerBindings =
        dopantProducerBindings(context, infosBySpecBuilder.values());
    return ImmutableList.of(
        new BindingsContainer(
            "Dopants",
            Stream.of(
                    dopantSpecBindings.stream(),
                    dopantConfigBindings.stream(),
                    dopantAnnoBindings.stream(),
                    Stream.of(appBootstrapBinding),
                    Stream.of(dopantSetBinding))
                .flatMap(identity())
                .collect(toImmutableList())),
        new BindingsContainer(ImmutableList.copyOf(dopantProducerBindings)));
  }

  private @NonNull List<Binding> dopantSpecBindings(
      LatticeCodegenContext context, Collection<DopantTypesInfo> dopantTypesInfos) {
    List<Binding> bindings = new ArrayList<>();
    for (DopantTypesInfo dopantTypesInfo : dopantTypesInfos) {
      ProcessingEnvironment processingEnv = context.codeGenUtility().codegenUtil().processingEnv();

      List<ExecutableElement> autoConfigureMethods = dopantTypesInfo.autoConfigurers();
      List<TypeMirror> dependencies = new ArrayList<>();
      List<? extends VariableElement> parameters = dopantTypesInfo.dopingMethod().getParameters();
      for (VariableElement parameter : parameters) {
        dependencies.add(parameter.asType());
      }
      autoConfigureMethods.stream()
          .map(Element::getEnclosingElement)
          .map(Element::asType)
          .forEach(dependencies::add);
      autoConfigureMethods.forEach(
          autoConfigureMethod -> {
            for (VariableElement parameter : autoConfigureMethod.getParameters()) {
              if (parameter.getAnnotation(AutoConfigure.class) == null) {
                dependencies.add(parameter.asType());
              }
            }
          });
      bindings.add(
          new ProviderMethod(
              dopantTypesInfo.dopantSpecElement().getSimpleName().toString(),
              TypeName.get(dopantTypesInfo.dopantSpecType()),
              dependencies.stream()
                  .map(
                      typeMirror ->
                          ParameterSpec.builder(
                                  TypeName.get(typeMirror), variableName(typeMirror, processingEnv))
                              .build())
                  .toList(),
              CodeBlock.builder()
                  .addNamed(
"""
    var _dopantSpecBuilder = $appType:T.$dopingMethod:L($dopingMethodParams:L);
    log.info("Auto-configuring '$dopantSpecType:L' : Start");
    $autoConfiguration:L
    log.info("Auto-configuring '$dopantSpecType:L' : End");
    return _dopantSpecBuilder._buildSpec();
""",
                      ofEntries(
                          entry("appType", context.latticeAppTypeElement().asType()),
                          entry("dopingMethod", dopantTypesInfo.dopingMethod().getSimpleName()),
                          entry(
                              "dopingMethodParams",
                              parameters.stream()
                                  .map(VariableElement::asType)
                                  .map(
                                      typeMirror ->
                                          CodeBlock.of(
                                              "$L", variableName(typeMirror, processingEnv)))
                                  .collect(CodeBlock.joining(", "))),
                          entry(
                              "dopantSpecType",
                              dopantTypesInfo.dopantSpecElement().getSimpleName()),
                          entry(
                              "autoConfiguration",
                              autoConfigureMethods.stream()
                                  .map(
                                      executableElement -> {
                                        Element configurer =
                                            executableElement.getEnclosingElement();
                                        return CodeBlock.builder()
                                            .addStatement(
                                                """
                                                    log.info("Auto-configuring '$L' with '$L' : Start")""",
                                                dopantTypesInfo.dopantSpecElement().getSimpleName(),
                                                configurer.getSimpleName())
                                            .addStatement(
                                                "$L.$L($L)",
                                                variableName(configurer.asType(), processingEnv),
                                                executableElement.getSimpleName().toString(),
                                                executableElement.getParameters().stream()
                                                    .map(
                                                        elem -> {
                                                          boolean isAutoConfig =
                                                              elem.getAnnotation(
                                                                      AutoConfigure.class)
                                                                  != null;
                                                          return CodeBlock.of(
                                                              "$L",
                                                              isAutoConfig
                                                                  ? "_dopantSpecBuilder"
                                                                  : variableName(
                                                                      elem.asType(),
                                                                      processingEnv));
                                                        })
                                                    .collect(CodeBlock.joining(", ")))
                                            .addStatement(
                                                """
                                                    log.info("Auto-configuring '$L' with '$L' : End")""",
                                                dopantTypesInfo.dopantSpecElement().getSimpleName(),
                                                configurer.getSimpleName())
                                            .build();
                                      })
                                  .collect(CodeBlock.joining("\n")))))
                  .build(),
              AnnotationSpec.builder(Singleton.class).build()));
    }
    return bindings;
  }

  private List<Binding> dopantConfigBindings(
      LatticeCodegenContext context, Collection<DopantTypesInfo> dopantTypesInfos) {
    List<Binding> bindings = new ArrayList<>();
    for (DopantTypesInfo dopantTypesInfo : dopantTypesInfos) {
      String latticeAppConfigVarName = lowerCaseFirstChar(LatticeAppConfig.class.getSimpleName());
      TypeMirror dopantConfigType = dopantTypesInfo.dopantConfigType();
      if (context
          .codeGenUtility()
          .codegenUtil()
          .isRawAssignable(dopantConfigType, NoConfiguration.class)) {
        // This dopant has no configuration.
        continue;
      }
      bindings.add(
          new ProviderMethod(
              dopantTypesInfo.dopantConfigElement().getSimpleName().toString(),
              TypeName.get(dopantConfigType)
                  .annotated(AnnotationSpec.builder(Nullable.class).build()),
              List.of(
                  ParameterSpec.builder(LatticeAppConfig.class, latticeAppConfigVarName).build()),
              CodeBlock.of(
                  "return ($T) $L.configsByDopantType().get($S);",
                  dopantConfigType,
                  latticeAppConfigVarName,
                  dopantTypesInfo.dopantTypeString()),
              AnnotationSpec.builder(Singleton.class).build()));
    }
    return bindings;
  }

  private List<Binding> dopantAnnoBindings(
      LatticeCodegenContext context, Collection<DopantTypesInfo> dopantTypesInfos) {
    List<Binding> bindings = new ArrayList<>();
    for (DopantTypesInfo dopantTypesInfo : dopantTypesInfos) {
      TypeMirror dopantAnnoType = dopantTypesInfo.dopantAnnoType();
      if (context
          .codeGenUtility()
          .codegenUtil()
          .isRawAssignable(dopantAnnoType, NoAnnotation.class)) {
        // This dopant has no annotation.
        continue;
      }
      bindings.add(
          new ProviderMethod(
              dopantTypesInfo.dopantAnnoElement().getSimpleName().toString(),
              TypeName.get(dopantAnnoType),
              List.of(),
              CodeBlock.of(
                  "return $T.class.getAnnotation($T.class);",
                  context.latticeAppTypeElement().asType(),
                  dopantAnnoType),
              AnnotationSpec.builder(Singleton.class).build()));
    }
    return bindings;
  }

  private Binding appBootstrapBinding(Collection<DopantTypesInfo> dopantTypesInfos) {
    List<ParameterSpec> dependencies =
        dopantTypesInfos.stream()
            .map(DopantTypesInfo::dopantSpecElement)
            .map(
                element ->
                    ParameterSpec.builder(
                            TypeName.get(element.asType()),
                            lowerCaseFirstChar(element.getSimpleName().toString()))
                        .build())
            .toList();
    return new ProviderMethod(
        LatticeAppBootstrap.class.getSimpleName(),
        ClassName.get(LatticeAppBootstrap.class),
        dependencies,
        CodeBlock.of(
            "return new $T($L);",
            LatticeAppBootstrap.class,
            dependencies.stream()
                .map(p -> CodeBlock.of("$L", p.name))
                .collect(CodeBlock.joining(", "))),
        AnnotationSpec.builder(Singleton.class).build());
  }

  private Binding dopantSetBinding(Collection<DopantTypesInfo> dopantTypesInfos) {
    List<ParameterSpec> dependencies =
        dopantTypesInfos.stream()
            .map(DopantTypesInfo::dopantElem)
            .map(
                element ->
                    ParameterSpec.builder(
                            TypeName.get(element.asType()),
                            lowerCaseFirstChar(element.getSimpleName().toString()))
                        .build())
            .toList();
    return new ProviderMethod(
        LatticeDopantSet.class.getSimpleName(),
        ClassName.get(LatticeDopantSet.class),
        dependencies,
        CodeBlock.of(
            "return new $T($L);",
            LatticeDopantSet.class,
            dependencies.stream()
                .map(p -> CodeBlock.of("$L", p.name))
                .collect(CodeBlock.joining(", "))),
        AnnotationSpec.builder(Singleton.class).build());
  }

  private List<Binding> dopantProducerBindings(
      LatticeCodegenContext context, Collection<DopantTypesInfo> dopantTypesInfos) {
    List<Binding> bindings = new ArrayList<>();
    for (DopantTypesInfo dopantTypesInfo : dopantTypesInfos) {
      TypeElement dopantElem = dopantTypesInfo.dopantElem();
      dopantElem.getEnclosedElements().stream()
          .filter(element -> element instanceof ExecutableElement)
          .map(ExecutableElement.class::cast)
          .filter(element -> element.getAnnotation(Produces.class) != null)
          .filter(element -> element.getReturnType().getKind() != TypeKind.VOID)
          .forEach(
              element -> {
                List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
                Produces producesAnno = element.getAnnotation(Produces.class);
                List<AnnotationMirror> qualifierAnnotations =
                    annotationMirrors.stream()
                        .filter(
                            annotationMirror ->
                                annotationMirror
                                        .getAnnotationType()
                                        .asElement()
                                        .getAnnotation(Qualifier.class)
                                    != null)
                        .collect(Collectors.toList());
                String dopantVarName = lowerCaseFirstChar(dopantElem.getSimpleName().toString());
                List<ParameterSpec> additionalParams =
                    element.getParameters().stream()
                        .map(
                            parameter ->
                                ParameterSpec.builder(
                                        TypeName.get(parameter.asType()),
                                        lowerCaseFirstChar(parameter.getSimpleName().toString()))
                                    .build())
                        .toList();
                List<ParameterSpec> bindingParams =
                    Stream.concat(
                            Stream.of(
                                ParameterSpec.builder(
                                        TypeName.get(dopantElem.asType()), dopantVarName)
                                    .build()),
                            additionalParams.stream())
                        .toList();

                TypeMirror returnType = element.getReturnType();

                List<AnnotationSpec> annotationSpecs =
                    new ArrayList<>(
                        qualifierAnnotations.stream().map(AnnotationSpec::get).toList());

                TypeMirror scopedTypeMirror =
                    context
                        .codeGenUtility()
                        .codegenUtil()
                        .getTypeFromAnnotationMember(producesAnno::inScope);
                TypeElement scopeElem =
                    (TypeElement)
                        context
                            .codeGenUtility()
                            .processingEnv()
                            .getTypeUtils()
                            .asElement(scopedTypeMirror);
                if (!ClassName.get(scopeElem).equals(ClassName.get(NoScope.class))
                    && scopeElem.getAnnotation(Scope.class) != null
                    && scopeElem.getAnnotation(NormalScope.class) != null) {
                  context
                      .codeGenUtility()
                      .codegenUtil()
                      .error(
                          "@Produces(scope=) class must have be scope (must have @jakarta.inject.Scope or @NormalScope annotation)");
                }

                bindings.add(
                    new ProviderMethod(
                        variableName(returnType, context.codeGenUtility().processingEnv()),
                        TypeName.get(returnType),
                        bindingParams,
                        CodeBlock.of(
                            "return $L.$L($L);",
                            dopantVarName,
                            element.getSimpleName(),
                            additionalParams.stream()
                                .map(p -> CodeBlock.of("$L", p.name))
                                .collect(CodeBlock.joining(", "))),
                        annotationSpecs,
                        AnnotationSpec.builder(ClassName.get(scopeElem)).build()));
              });
    }
    return bindings;
  }

  private String variableName(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    TypeElement element = (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror);
    return lowerCaseFirstChar(element.getSimpleName().toString());
  }

  private void validateDopingMethod(ExecutableElement dopingMethod, LatticeCodegenContext context) {
    if (!dopingMethod.getModifiers().contains(STATIC)) {
      context.codeGenUtility().codegenUtil().error("Doping method must be static", dopingMethod);
    }
    if (dopingMethod.getModifiers().contains(PRIVATE)) {
      context
          .codeGenUtility()
          .codegenUtil()
          .error("Doping method must not be private", dopingMethod);
    }
    TypeMirror returnType = dopingMethod.getReturnType();
    if (!context
        .codeGenUtility()
        .codegenUtil()
        .isRawAssignable(returnType, DopantSpecBuilder.class)) {
      context
          .codeGenUtility()
          .codegenUtil()
          .error("Doping method must return an instance of DopantSpecBuilder", dopingMethod);
    }
    Element returnTypeElement =
        context.codeGenUtility().codegenUtil().processingEnv().getTypeUtils().asElement(returnType);
    if (!(returnTypeElement instanceof TypeElement typeElement)) {
      context
          .codeGenUtility()
          .codegenUtil()
          .error("Doping method must return an object instance", dopingMethod);
      return;
    }
    if (!typeElement.getModifiers().contains(FINAL)) {
      context
          .codeGenUtility()
          .codegenUtil()
          .error("Return type of doping method must be a final class", dopingMethod);
    }
  }

  record DopantTypesInfo(
      String dopantTypeString,
      TypeMirror dopantType,
      TypeElement dopantElem,
      TypeMirror dopantSpecBuilderType,
      TypeElement dopantSpecBuilderElem,
      TypeMirror dopantSpecType,
      TypeElement dopantSpecElement,
      TypeMirror dopantConfigType,
      TypeElement dopantConfigElement,
      TypeMirror dopantAnnoType,
      TypeElement dopantAnnoElement,
      ExecutableElement dopingMethod,
      List<ExecutableElement> autoConfigurers) {}
}
