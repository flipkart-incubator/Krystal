package com.flipkart.krystal.vajram.graphql.client.codegen;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.graphql.client.codegen.ClientCodeGenConstants.QUERY_FACADE_SUFFIX;
import static com.flipkart.krystal.vajram.graphql.client.codegen.ClientCodeGenConstants.RESPONSE_WRAPPER_SUFFIX;
import static java.util.Objects.requireNonNull;
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
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlFragment;
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
import java.util.regex.Pattern;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Generates a {@code <OperationRoot>QueryFacade} class for a single {@code @ForGraphQlOpReq}
 * -annotated variables model - see {@link GraphQlFacadeProcessor}.
 */
final class GraphQlSpecRequestGen {

  private static final String TYPENAME_META_FIELD = "__typename";

  // GraphQL `Name` grammar: https://spec.graphql.org/October2021/#sec-Names
  private static final Pattern GRAPHQL_NAME_PATTERN = Pattern.compile("[_A-Za-z][_0-9A-Za-z]*");

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

  /**
   * A single entry in a selection set - either a field selection ({@code alias}/{@code
   * fieldName}/{@code args}/{@code nested} populated, {@code fragmentSpread} null) or a fragment
   * spread ({@code fragmentSpread} populated, all other fields null/empty).
   */
  private record Selection(
      @Nullable String alias,
      @Nullable String fieldName,
      List<ArgBinding> args,
      List<Selection> nested,
      @Nullable String fragmentSpread) {

    static Selection field(
        String alias, String fieldName, List<ArgBinding> args, List<Selection> nested) {
      return new Selection(alias, fieldName, args, nested, null);
    }

    static Selection spread(String fragmentName) {
      return new Selection(null, null, List.of(), List.of(), fragmentName);
    }
  }

  /** A top-level {@code fragment <name> on <typeCondition> { ... }} definition. */
  private record Fragment(
      String name, String typeCondition, String declaringInterface, List<Selection> selections) {}

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
    if (operationRoot.getAnnotation(ModelRoot.class) == null) {
      util.error(
          "Model root type %s does not have @ModelRoot annotation".formatted(operationRoot),
          operationRoot);
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
    Map<String, Fragment> fragments = new LinkedHashMap<>();
    List<Selection> rootSelections =
        buildOwnSelections(
            operationRoot, rootType, variablesModel, ctx, varDecls, hasError, fragments);
    if (hasError.get()) {
      return;
    }

    String opName = operationRoot.getSimpleName().toString();
    String varDeclsStr =
        varDecls.entrySet().stream()
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(joining(", "));
    String varDeclsParen = varDeclsStr.isEmpty() ? "" : "(" + varDeclsStr + ")";

    String fragmentsCompact =
        fragments.values().stream().map(this::renderFragmentCompact).collect(joining(" "));
    String compactSelections =
        rootSelections.stream().map(this::renderCompact).collect(joining(" "));
    String compactQuery =
        (fragmentsCompact.isEmpty() ? "" : fragmentsCompact + " ")
            + "%s %s%s { %s }".formatted(opKeyword, opName, varDeclsParen, compactSelections);

    String fragmentsPretty =
        fragments.values().stream().map(this::renderFragmentPretty).collect(joining("\n\n"));
    String prettySelections =
        rootSelections.stream().map(s -> renderPretty(s, 1)).collect(joining("\n"));
    String prettyQuery =
        (fragmentsPretty.isEmpty() ? "" : fragmentsPretty + "\n\n")
            + "%s %s%s {\n%s\n}".formatted(opKeyword, opName, varDeclsParen, prettySelections);

    emitFacade(operationRoot, variablesModel, opName, compactQuery, prettyQuery);
    emitResponseWrapper(operationRoot);
  }

  private Selection buildSelection(
      ExecutableElement method,
      ObjectTypeDefinition parentType,
      TypeElement variablesModel,
      SchemaContext ctx,
      Map<String, String> varDecls,
      AtomicBoolean hasError,
      Map<String, Fragment> fragments) {
    String fieldName = resolveFieldName(method);
    String alias = method.getSimpleName().toString();

    if (TYPENAME_META_FIELD.equals(fieldName)) {
      // `__typename` is a GraphQL introspection meta-field valid on any composite type - it has
      // no `FieldDefinition` in the schema and takes no arguments, so it's handled directly
      // instead of going through schema field-existence/arg validation below.
      if (method.getAnnotationsByType(FieldArg.class).length > 0) {
        util.error(
            "@FieldArg is not supported on the '__typename' meta field (method '%s')"
                .formatted(method.getSimpleName()),
            method);
        hasError.set(true);
        return null;
      }
      TypeMirror typenameContent = method.getReturnType();
      if (util.isOptional(typenameContent)) {
        typenameContent = util.getOptionalInnerType(typenameContent);
      }
      if (util.isErrable(typenameContent)) {
        typenameContent = util.getErrableInnerType(typenameContent);
      }
      if (!TypeName.get(typenameContent).equals(TypeName.get(String.class))) {
        util.error(
            "Method '%s' is bound to the '__typename' meta field, which resolves to a String, but its return type is '%s'"
                .formatted(method.getSimpleName(), method.getReturnType()),
            method);
        hasError.set(true);
        return null;
      }
      return Selection.field(alias, fieldName, List.of(), List.of());
    }

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
        nested =
            buildOwnSelections(
                contentType,
                nestedGqlType.get(),
                variablesModel,
                ctx,
                varDecls,
                hasError,
                fragments);
      }
    }

    return Selection.field(alias, fieldName, argBindings, nested);
  }

  /**
   * Builds the selection set for {@code typeElem} against {@code contextType} - fields declared
   * directly on {@code typeElem} (inlined), plus a {@code ...FragmentName} spread for each
   * supertype in {@code typeElem}'s {@code extends} clause that qualifies as a
   * {@code @GraphQlFragment} (registering the fragment's own definition into {@code fragments} on
   * first use). Used for the operation root, for nested {@code @GraphQlRequest} types, and
   * recursively for fragment bodies themselves.
   */
  private List<Selection> buildOwnSelections(
      TypeElement typeElem,
      ObjectTypeDefinition contextType,
      TypeElement variablesModel,
      SchemaContext ctx,
      Map<String, String> varDecls,
      AtomicBoolean hasError,
      Map<String, Fragment> fragments) {
    List<Selection> selections = new ArrayList<>();
    for (ExecutableElement ownMethod : util.getDeclaredModelFieldsForCodegen(typeElem)) {
      Selection s =
          buildSelection(
              ownMethod, contextType, variablesModel, ctx, varDecls, hasError, fragments);
      if (s != null) {
        selections.add(s);
      }
    }
    collectFragmentSelections(
        typeElem, contextType, variablesModel, ctx, varDecls, hasError, fragments, selections);
    return selections;
  }

  /**
   * Walks {@code typeElem.getInterfaces()} looking for {@code @GraphQlFragment} supertypes,
   * appending a {@code Selection.spread(name)} to {@code outSelections} for each one found, and
   * registering its definition into {@code fragments} on first use.
   */
  private void collectFragmentSelections(
      TypeElement typeElem,
      ObjectTypeDefinition contextType,
      TypeElement variablesModel,
      SchemaContext ctx,
      Map<String, String> varDecls,
      AtomicBoolean hasError,
      Map<String, Fragment> fragments,
      List<Selection> outSelections) {
    for (TypeMirror iface : typeElem.getInterfaces()) {
      if (!(iface instanceof DeclaredType dt)
          || !(dt.asElement() instanceof TypeElement ifaceElem)) {
        continue;
      }
      boolean declaredAsFragment = ifaceElem.getAnnotation(GraphQlFragment.class) != null;
      // `iface.getAnnotation(GraphQlFragment.class)` is unreliable for a TYPE_USE annotation on
      // an `extends`-clause reference in some javac versions - it's present in
      // `getAnnotationMirrors()` but not always surfaced by the `getAnnotation(Class)`
      // convenience method, so match the mirror by qualified name instead.
      boolean usedAsFragment = hasFragmentAnnotationMirror(iface);
      if (!declaredAsFragment && !usedAsFragment) {
        if (ifaceElem.getQualifiedName().contentEquals(Model.class.getCanonicalName())) {
          // The `Model` marker interface every model extends - no fragment involved.
          continue;
        }
        util.error(
            ("Interface '%s' extends '%s', which is not annotated with @GraphQlFragment. Only"
                    + " the '%s' marker interface may be extended without declaring a fragment;"
                    + " annotate '%s' (both its own declaration and this extends-clause"
                    + " reference) with @GraphQlFragment if it's meant to be spread as a"
                    + " fragment")
                .formatted(
                    typeElem.getQualifiedName(),
                    ifaceElem.getQualifiedName(),
                    Model.class.getCanonicalName(),
                    ifaceElem.getQualifiedName()),
            typeElem);
        hasError.set(true);
        continue;
      }
      if (declaredAsFragment != usedAsFragment) {
        util.error(
            ("Interface '%s' extends '%s' as a fragment inconsistently: both the fragment's own"
                    + " declaration and the extends-clause reference must be annotated with"
                    + " @GraphQlFragment")
                .formatted(typeElem.getQualifiedName(), ifaceElem.getQualifiedName()),
            typeElem);
        hasError.set(true);
        continue;
      }

      GraphQlFragment fragAnno = requireNonNull(ifaceElem.getAnnotation(GraphQlFragment.class));
      String fragmentName =
          fragAnno.name().isBlank() ? ifaceElem.getSimpleName().toString() : fragAnno.name();
      if (!GRAPHQL_NAME_PATTERN.matcher(fragmentName).matches()) {
        util.error(
            ("@GraphQlFragment(name = \"%s\") on '%s' is not a valid GraphQL name - it must match"
                    + " [_A-Za-z][_0-9A-Za-z]*")
                .formatted(fragmentName, ifaceElem.getQualifiedName()),
            typeElem);
        hasError.set(true);
        continue;
      }

      String declaringInterface = ifaceElem.getQualifiedName().toString();
      Fragment existing = fragments.get(fragmentName);
      if (existing == null) {
        List<Selection> fragSelections =
            buildOwnSelections(
                ifaceElem, contextType, variablesModel, ctx, varDecls, hasError, fragments);
        fragments.put(
            fragmentName,
            new Fragment(fragmentName, contextType.getName(), declaringInterface, fragSelections));
      } else if (!existing.declaringInterface().equals(declaringInterface)) {
        util.error(
            ("Fragment name '%s' is declared by two different interfaces ('%s' and '%s') within"
                    + " the same operation - fragment names must be unique per operation")
                .formatted(fragmentName, existing.declaringInterface(), declaringInterface),
            typeElem);
        hasError.set(true);
        continue;
      } else if (!existing.typeCondition().equals(contextType.getName())) {
        util.error(
            ("Fragment '%s' is used against conflicting GraphQL types '%s' and '%s' within the"
                    + " same operation")
                .formatted(fragmentName, existing.typeCondition(), contextType.getName()),
            typeElem);
        hasError.set(true);
        continue;
      }
      outSelections.add(Selection.spread(fragmentName));
    }
  }

  private static boolean hasFragmentAnnotationMirror(TypeMirror typeMirror) {
    String fragmentAnnoName = GraphQlFragment.class.getCanonicalName();
    for (AnnotationMirror mirror : typeMirror.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().asElement() instanceof TypeElement annoElem
          && annoElem.getQualifiedName().contentEquals(fragmentAnnoName)) {
        return true;
      }
    }
    return false;
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
    if (s.fragmentSpread() != null) {
      return "..." + s.fragmentSpread();
    }
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
    if (s.fragmentSpread() != null) {
      return indent + "..." + s.fragmentSpread();
    }
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

  private String renderFragmentCompact(Fragment f) {
    String body = f.selections().stream().map(this::renderCompact).collect(joining(" "));
    return "fragment %s on %s { %s }".formatted(f.name(), f.typeCondition(), body);
  }

  private String renderFragmentPretty(Fragment f) {
    String body = f.selections().stream().map(s -> renderPretty(s, 1)).collect(joining("\n"));
    return "fragment %s on %s {\n%s\n}".formatted(f.name(), f.typeCondition(), body);
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
                    // Propagate `pure` from the operation root: since `data` is typed as the
                    // operation root itself, the wrapper can only be pure if the root (and
                    // everything it transitively references) is pure too.
                    .addMember("pure", "$L", operationRoot.getAnnotation(ModelRoot.class).pure())
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
