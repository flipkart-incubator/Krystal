package com.flipkart.krystal.vajram.graphql.client.codegen;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.graphql.client.codegen.ClientCodeGenConstants.QUERY_FACADE_SUFFIX;
import static com.flipkart.krystal.vajram.graphql.client.codegen.ClientCodeGenConstants.RESPONSE_WRAPPER_SUFFIX;
import static java.util.stream.Collectors.joining;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.GraphQlSpecRequest;
import com.flipkart.krystal.vajram.graphql.client.api.Field;
import com.flipkart.krystal.vajram.graphql.client.api.FieldArg;
import com.flipkart.krystal.vajram.graphql.client.api.ForGraphQlOpReq;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlOpRequest;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
import com.flipkart.krystal.vajram.graphql.schema.ArgTypeValidator;
import com.flipkart.krystal.vajram.graphql.schema.SchemaLoader;
import com.flipkart.krystal.vajram.graphql.schema.SchemaLocator;
import com.flipkart.krystal.vajram.graphql.schema.SharedTypeNameResolver;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.Type;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Generates a {@code <OperationRoot>QueryFacade} class for a single {@code @ForGraphQlOpReq}
 * -annotated variables model - see {@link GraphQlFacadeProcessor}.
 */
final class GraphQlSpecRequestGen {

  private final CodeGenUtility util;
  private final Map<String, SchemaContext> schemaCache;

  GraphQlSpecRequestGen(CodeGenUtility util, Map<String, SchemaContext> schemaCache) {
    this.util = util;
    this.schemaCache = schemaCache;
  }

  record SchemaContext(
      TypeDefinitionRegistry registry,
      SharedTypeNameResolver typeNameResolver,
      ArgTypeValidator argValidator) {}

  private record ArgBinding(String argName, String varRef) {}

  private record Selection(
      String alias, String fieldName, List<ArgBinding> args, List<Selection> nested) {}

  void generate(TypeElement variablesModel) {
    ForGraphQlOpReq forGraphQlOpReq = variablesModel.getAnnotation(ForGraphQlOpReq.class);
    if (forGraphQlOpReq == null) {
      return;
    }
    TypeElement operationRoot = util.getTypeElemFromAnnotationMember(forGraphQlOpReq::value);
    GraphQlOpRequest graphQlOpRequest = operationRoot.getAnnotation(GraphQlOpRequest.class);
    if (graphQlOpRequest == null) {
      util.error(
          "Operation root '%s' referenced by @ForGraphQlOpReq must be annotated with @GraphQlOpRequest"
              .formatted(operationRoot.getQualifiedName()),
          variablesModel);
      return;
    }

    File schemaFile = SchemaLocator.locate(util, graphQlOpRequest.schemaFilePath()).toFile();
    if (!schemaFile.exists()) {
      util.error(
          "GraphQL schema file not found: '%s' (resolved from"
              + " @GraphQlOpRequest(schemaFilePath=\"%s\"))"
                  .formatted(schemaFile, graphQlOpRequest.schemaFilePath()),
          operationRoot);
      return;
    }

    SchemaContext ctx =
        schemaCache.computeIfAbsent(
            schemaFile.getAbsolutePath(),
            k -> {
              TypeDefinitionRegistry registry = SchemaLoader.parse(schemaFile);
              SharedTypeNameResolver typeNameResolver =
                  new SharedTypeNameResolver(registry, SchemaLoader.getRootPackageName(registry));
              return new SchemaContext(
                  registry, typeNameResolver, new ArgTypeValidator(typeNameResolver));
            });

    Optional<SchemaDefinition> schemaDefinition = ctx.registry().schemaDefinition();
    if (schemaDefinition.isEmpty()) {
      util.error("GraphQL schema has no schema definition", operationRoot);
      return;
    }

    Map<String, ObjectTypeDefinition> rootTypesByOperationKeyword = new LinkedHashMap<>();
    for (OperationTypeDefinition otd : schemaDefinition.get().getOperationTypeDefinitions()) {
      ctx.registry()
          .getType(otd.getTypeName().getName(), ObjectTypeDefinition.class)
          .ifPresent(def -> rootTypesByOperationKeyword.put(otd.getName(), def));
    }
    if (rootTypesByOperationKeyword.isEmpty()) {
      util.error("GraphQL schema declares no query/mutation/subscription root type", operationRoot);
      return;
    }

    List<ExecutableElement> rootMethods = util.getModelFieldsForCodegen(operationRoot);
    if (rootMethods.isEmpty()) {
      util.error(
          "Operation root '%s' has no selections (model fields)"
              .formatted(operationRoot.getQualifiedName()),
          operationRoot);
      return;
    }

    String firstFieldName = resolveFieldName(rootMethods.get(0));
    String opKeyword = null;
    ObjectTypeDefinition rootType = null;
    for (Map.Entry<String, ObjectTypeDefinition> e : rootTypesByOperationKeyword.entrySet()) {
      if (e.getValue().getFieldDefinitions().stream()
          .anyMatch(f -> f.getName().equals(firstFieldName))) {
        opKeyword = e.getKey();
        rootType = e.getValue();
        break;
      }
    }
    if (opKeyword == null) {
      Map.Entry<String, ObjectTypeDefinition> fallback =
          rootTypesByOperationKeyword.entrySet().iterator().next();
      opKeyword = fallback.getKey();
      rootType = fallback.getValue();
    }

    Map<String, String> varDecls = new LinkedHashMap<>();
    AtomicBoolean hasError = new AtomicBoolean(false);
    List<Selection> rootSelections = new ArrayList<>();
    for (ExecutableElement rootMethod : rootMethods) {
      Selection selection =
          buildSelection(rootMethod, rootType, variablesModel, ctx, varDecls, hasError);
      if (selection != null) {
        rootSelections.add(selection);
      }
    }
    if (hasError.get()) {
      return;
    }

    String opName = operationRoot.getSimpleName().toString();
    String varDeclsStr =
        varDecls.entrySet().stream()
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(joining(", "));
    String varDeclsParen = varDeclsStr.isEmpty() ? "" : "(" + varDeclsStr + ")";

    String compactSelections =
        rootSelections.stream().map(this::renderCompact).collect(joining(" "));
    String compactQuery =
        "%s %s%s { %s }".formatted(opKeyword, opName, varDeclsParen, compactSelections);

    String prettySelections =
        rootSelections.stream().map(s -> renderPretty(s, 1)).collect(joining("\n"));
    String prettyQuery =
        "%s %s%s {\n%s\n}".formatted(opKeyword, opName, varDeclsParen, prettySelections);

    emitFacade(operationRoot, variablesModel, opName, compactQuery, prettyQuery);
    emitResponseWrapper(operationRoot);
  }

  private Selection buildSelection(
      ExecutableElement method,
      ObjectTypeDefinition parentType,
      TypeElement variablesModel,
      SchemaContext ctx,
      Map<String, String> varDecls,
      AtomicBoolean hasError) {
    String fieldName = resolveFieldName(method);
    String alias = method.getSimpleName().toString();
    Optional<FieldDefinition> fieldDefOpt =
        parentType.getFieldDefinitions().stream()
            .filter(f -> f.getName().equals(fieldName))
            .findFirst();
    if (fieldDefOpt.isEmpty()) {
      util.error(
          "GraphQL field '%s' (resolved from method '%s') does not exist on type '%s'"
              .formatted(fieldName, method.getSimpleName(), parentType.getName()),
          method);
      hasError.set(true);
      return null;
    }
    FieldDefinition fieldDef = fieldDefOpt.get();

    List<ArgBinding> argBindings = new ArrayList<>();
    for (FieldArg fieldArg : method.getAnnotationsByType(FieldArg.class)) {
      Optional<InputValueDefinition> argOpt =
          ctx.argValidator().resolveArg(fieldDef, fieldArg.name());
      if (argOpt.isEmpty()) {
        util.error(
            "GraphQL field '%s' has no argument named '%s' (declared via @FieldArg)"
                .formatted(fieldName, fieldArg.name()),
            method);
        hasError.set(true);
        continue;
      }
      InputValueDefinition arg = argOpt.get();
      Optional<ExecutableElement> varMethod =
          util.getModelFieldsForCodegen(variablesModel).stream()
              .filter(m -> m.getSimpleName().contentEquals(fieldArg.useVariable()))
              .findFirst();
      if (varMethod.isEmpty()) {
        util.error(
            "@FieldArg(useVariable = \"%s\") on method '%s' does not resolve to any field on variables model '%s'"
                .formatted(
                    fieldArg.useVariable(),
                    method.getSimpleName(),
                    variablesModel.getQualifiedName()),
            method);
        hasError.set(true);
        continue;
      }
      ExecutableElement varMethodElem = varMethod.get();
      TypeMirror returnType = varMethodElem.getReturnType();
      TypeMirror effective =
          util.isOptional(returnType)
              ? util.getOptionalInnerType(returnType)
              : util.isErrable(returnType) ? util.getErrableInnerType(returnType) : returnType;
      TypeName javaFieldType = TypeName.get(effective);
      IfAbsent ifAbsent =
          util.getIfAbsent(varMethodElem, variablesModel.getAnnotation(ModelRoot.class));
      boolean effectivelyNonOptional = FAIL.equals(ifAbsent.value());
      Optional<String> error =
          ctx.argValidator().validate(arg, javaFieldType, effectivelyNonOptional);
      if (error.isPresent()) {
        util.error(error.get(), method);
        hasError.set(true);
        continue;
      }
      varDecls.putIfAbsent("$" + fieldArg.useVariable(), printGraphQlType(arg.getType()));
      argBindings.add(new ArgBinding(fieldArg.name(), "$" + fieldArg.useVariable()));
    }

    List<Selection> nested = List.of();
    TypeMirror content = method.getReturnType();
    if (util.isOptional(content)) {
      content = util.getOptionalInnerType(content);
    }
    if (util.isErrable(content)) {
      content = util.getErrableInnerType(content);
    }
    if (util.isListType(content)) {
      content = util.getContentType(content);
    }
    if (content.getKind() == TypeKind.DECLARED
        && ((DeclaredType) content).asElement() instanceof TypeElement contentType
        && contentType.getAnnotation(GraphQlRequest.class) != null) {
      Optional<ObjectTypeDefinition> nestedGqlType =
          resolveObjectType(ctx.registry(), fieldDef.getType());
      if (nestedGqlType.isEmpty()) {
        util.error(
            "Field '%s' is bound to a Java model marked @GraphQlRequest, but its GraphQL schema type '%s' is not an object type"
                .formatted(fieldName, fieldDef.getType()),
            method);
        hasError.set(true);
      } else {
        List<Selection> built = new ArrayList<>();
        for (ExecutableElement nestedMethod : util.getModelFieldsForCodegen(contentType)) {
          Selection s =
              buildSelection(
                  nestedMethod, nestedGqlType.get(), variablesModel, ctx, varDecls, hasError);
          if (s != null) {
            built.add(s);
          }
        }
        nested = built;
      }
    }

    return new Selection(alias, fieldName, argBindings, nested);
  }

  private static String resolveFieldName(ExecutableElement method) {
    Field field = method.getAnnotation(Field.class);
    if (field != null && !field.name().isBlank()) {
      return field.name();
    }
    return method.getSimpleName().toString();
  }

  private static Optional<ObjectTypeDefinition> resolveObjectType(
      TypeDefinitionRegistry registry, Type<?> type) {
    while (type instanceof NonNullType nonNullType) {
      type = nonNullType.getType();
    }
    while (type instanceof ListType listType) {
      type = listType.getType();
      while (type instanceof NonNullType nonNullType) {
        type = nonNullType.getType();
      }
    }
    if (type instanceof graphql.language.TypeName typeName) {
      return registry.getType(typeName.getName(), ObjectTypeDefinition.class);
    }
    return Optional.empty();
  }

  private static String printGraphQlType(Type<?> type) {
    if (type instanceof NonNullType nonNullType) {
      return printGraphQlType(nonNullType.getType()) + "!";
    }
    if (type instanceof ListType listType) {
      return "[" + printGraphQlType(listType.getType()) + "]";
    }
    if (type instanceof graphql.language.TypeName typeName) {
      return typeName.getName();
    }
    throw new IllegalArgumentException("Unknown GraphQL type: " + type);
  }

  private String renderCompact(Selection s) {
    String argsStr =
        s.args().isEmpty()
            ? ""
            : s.args().stream()
                .map(a -> a.argName() + ": " + a.varRef())
                .collect(joining(", ", "(", ")"));
    String prefix =
        s.alias().equals(s.fieldName()) ? s.fieldName() : s.alias() + ": " + s.fieldName();
    if (s.nested().isEmpty()) {
      return prefix + argsStr;
    }
    String nestedStr = s.nested().stream().map(this::renderCompact).collect(joining(" "));
    return prefix + argsStr + " { " + nestedStr + " }";
  }

  private String renderPretty(Selection s, int depth) {
    String indent = "  ".repeat(depth);
    String argsStr =
        s.args().isEmpty()
            ? ""
            : s.args().stream()
                .map(a -> a.argName() + ": " + a.varRef())
                .collect(joining(", ", "(", ")"));
    String prefix =
        s.alias().equals(s.fieldName()) ? s.fieldName() : s.alias() + ": " + s.fieldName();
    if (s.nested().isEmpty()) {
      return indent + prefix + argsStr;
    }
    String nestedStr =
        s.nested().stream().map(n -> renderPretty(n, depth + 1)).collect(joining("\n"));
    return indent + prefix + argsStr + " {\n" + nestedStr + "\n" + indent + "}";
  }

  private void emitFacade(
      TypeElement operationRoot,
      TypeElement variablesModel,
      String opName,
      String compactQuery,
      String prettyQuery) {
    ClassName variablesClassName = ClassName.get(variablesModel);
    String packageName = util.getPackageName(operationRoot);
    String facadeSimpleName = operationRoot.getSimpleName() + QUERY_FACADE_SUFFIX;

    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(facadeSimpleName)
            .addModifiers(PUBLIC, FINAL)
            .addJavadoc("<pre>\n$L\n</pre>\n", prettyQuery);

    classBuilder.addField(
        FieldSpec.builder(String.class, "QUERY", PRIVATE, STATIC, FINAL)
            .initializer("$L", "\"\"\"\n    " + compactQuery + "\"\"\"")
            .build());

    classBuilder.addMethod(
        MethodSpec.methodBuilder("of")
            .addModifiers(PUBLIC, STATIC)
            .returns(ClassName.get(GraphQlSpecRequest.class))
            .addParameter(variablesClassName, "variables")
            .addStatement(
                "return new $T(QUERY, $S, variables, $T.of())",
                GraphQlSpecRequest.class,
                opName,
                Map.class)
            .build());

    util.writeJavaFile(packageName, classBuilder.build(), operationRoot);
  }

  /**
   * Emits a new {@code @ModelRoot} source interface, {@code <OperationRoot>_GqlClientResp}, whose
   * single {@code data} field is typed as the operation root itself - a deserialization target for
   * a full GraphQL-over-HTTP response body, so the raw HTTP response can be deserialized directly
   * into a typed object in one pass, without first parsing into a generic tree and then converting
   * the {@code data} node separately. Declares the same {@code @SupportedModelProtocol}s as the
   * operation root, so it picks up whichever serde protocol(s) the operation itself uses; the
   * generated source is picked up by {@code ModelGenProcessor} in a later round like any other
   * hand-written model.
   */
  private void emitResponseWrapper(TypeElement operationRoot) {
    String packageName = util.getPackageName(operationRoot);
    ClassName operationRootType = ClassName.get(operationRoot);
    String wrapperSimpleName = operationRoot.getSimpleName() + RESPONSE_WRAPPER_SUFFIX;

    TypeSpec.Builder interfaceBuilder =
        TypeSpec.interfaceBuilder(wrapperSimpleName)
            .addModifiers(PUBLIC)
            .addSuperinterface(Model.class)
            .addAnnotation(
                AnnotationSpec.builder(ModelRoot.class)
                    .addMember("type", "$T.$L", ModelRoot.ModelType.class, "RESPONSE")
                    .build())
            .addJavadoc(
                "Deserialization target for a GraphQL-over-HTTP response body whose {@code data}"
                    + " matches {@link $T}.\n",
                operationRootType);

    for (SupportedModelProtocol protocol :
        operationRoot.getAnnotationsByType(SupportedModelProtocol.class)) {
      TypeElement protocolType = util.getTypeElemFromAnnotationMember(protocol::value);
      interfaceBuilder.addAnnotation(
          AnnotationSpec.builder(SupportedModelProtocol.class)
              .addMember("value", "$T.class", ClassName.get(protocolType))
              .build());
    }

    interfaceBuilder.addMethod(
        MethodSpec.methodBuilder("data")
            .addModifiers(PUBLIC, ABSTRACT)
            .returns(operationRootType)
            .build());

    util.writeJavaFile(packageName, interfaceBuilder.build(), operationRoot);
  }
}
