package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.vertx.sqlclient.Row;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;

/**
 * Generates an immutable implementation of a {@code @SELECT @ModelRoot} interface that maps a
 * Vert.x {@link Row} to the interface's getter methods.
 *
 * <p>For example, given:
 *
 * <pre>{@code
 * @ModelRoot
 * @SELECT(from = User.class)
 * public interface UserInfo extends Model {
 *   long id();
 *   String name();
 *   @Column("email") String contactEmail();
 *   Optional<String> phoneNumber();
 * }
 * }</pre>
 *
 * <p>this generator produces {@code UserInfo_Immut} backed by a {@link Row}.
 */
public class SelectResultGen {

  private static final String IMMUT_SUFFIX = "_Immut";
  private static final String SELECT_ANNO = "com.flipkart.krystal.vajram.ext.sql.statement.SELECT";
  private static final String MODEL_ROOT_ANNO = "com.flipkart.krystal.model.ModelRoot";
  private static final String COLUMN_ANNO = "com.flipkart.krystal.vajram.ext.sql.statement.Column";

  private final CodeGenUtility util;

  public SelectResultGen(CodeGenUtility util) {
    this.util = util;
  }

  public void generate(RoundEnvironment roundEnv) {
    TypeElement selectAnnoType = util.processingEnv().getElementUtils().getTypeElement(SELECT_ANNO);
    TypeElement modelRootAnnoType =
        util.processingEnv().getElementUtils().getTypeElement(MODEL_ROOT_ANNO);
    if (selectAnnoType == null || modelRootAnnoType == null) {
      return;
    }

    Set<? extends Element> selectElements = roundEnv.getElementsAnnotatedWith(selectAnnoType);
    Set<? extends Element> modelRootElements = roundEnv.getElementsAnnotatedWith(modelRootAnnoType);

    for (Element element : selectElements) {
      if (!modelRootElements.contains(element)) {
        continue;
      }
      if (!(element instanceof TypeElement typeElement)) {
        continue;
      }
      try {
        generateImmut(typeElement);
      } catch (Exception e) {
        util.error(
            "[SelectResultGen] Error generating immut for %s: %s"
                .formatted(typeElement.getQualifiedName(), e.getMessage()),
            typeElement);
      }
    }
  }

  private void generateImmut(TypeElement selectInterface) throws Exception {
    String pkg =
        util.processingEnv()
            .getElementUtils()
            .getPackageOf(selectInterface)
            .getQualifiedName()
            .toString();
    String simpleName = selectInterface.getSimpleName().toString();
    String immutName = simpleName + IMMUT_SUFFIX;
    ClassName rowType = ClassName.get(Row.class);
    ClassName immutClass = ClassName.get(pkg, immutName);
    ClassName interfaceClass = ClassName.get(pkg, simpleName);

    List<ExecutableElement> methods =
        ElementFilter.methodsIn(selectInterface.getEnclosedElements());

    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(immutName)
            .addModifiers(PUBLIC, FINAL)
            .addSuperinterface(interfaceClass)
            .addField(FieldSpec.builder(rowType, "row", PRIVATE, FINAL).build())
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addParameter(rowType, "row")
                    .addStatement("this.row = row")
                    .build());

    for (ExecutableElement method : methods) {
      TypeMirror returnType = method.getReturnType();
      String methodName = method.getSimpleName().toString();
      String colName = resolveColumnName(method);

      MethodSpec override = buildGetterMethod(methodName, returnType, colName);
      classBuilder.addMethod(override);
    }

    TypeSpec immutSpec = classBuilder.build();
    JavaFile javaFile = JavaFile.builder(pkg, immutSpec).build();

    StringWriter sw = new StringWriter();
    javaFile.writeTo(sw);

    JavaFileObject sourceFile =
        util.processingEnv().getFiler().createSourceFile(pkg + "." + immutName);
    try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
      out.println(sw);
    }
    util.note("Generated " + pkg + "." + immutName);
  }

  /** Returns the column name for a SELECT result method (may differ from method name). */
  private String resolveColumnName(ExecutableElement method) {
    String methodName = method.getSimpleName().toString();
    for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().toString().equals(COLUMN_ANNO)) {
        for (AnnotationValue value : mirror.getElementValues().values()) {
          String colName = (String) value.getValue();
          if (!colName.isEmpty()) {
            return colName;
          }
        }
      }
    }
    return methodName;
  }

  /** Builds an @Override getter method that reads from the Vert.x Row. */
  private MethodSpec buildGetterMethod(String methodName, TypeMirror returnType, String colName) {
    boolean isOptional = isOptionalType(returnType);
    TypeName javaReturnType = resolveTypeName(returnType);
    CodeBlock body;

    if (isOptional) {
      TypeMirror innerType = getOptionalInnerType(returnType);
      String getter = rowGetterCall(innerType, colName);
      body = CodeBlock.of("return $T.ofNullable($L)", Optional.class, getter);
    } else {
      body = CodeBlock.of("return $L", rowGetterCall(returnType, colName));
    }

    return MethodSpec.methodBuilder(methodName)
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(javaReturnType)
        .addStatement(body)
        .build();
  }

  private boolean isOptionalType(TypeMirror type) {
    if (type instanceof DeclaredType dt) {
      Element elem = dt.asElement();
      return elem instanceof TypeElement te
          && te.getQualifiedName().contentEquals("java.util.Optional");
    }
    return false;
  }

  private TypeMirror getOptionalInnerType(TypeMirror type) {
    if (type instanceof DeclaredType dt && !dt.getTypeArguments().isEmpty()) {
      return dt.getTypeArguments().get(0);
    }
    return type;
  }

  private TypeName resolveTypeName(TypeMirror type) {
    if (type.getKind() == TypeKind.LONG) return TypeName.LONG;
    if (type.getKind() == TypeKind.INT) return TypeName.INT;
    if (type.getKind() == TypeKind.BOOLEAN) return TypeName.BOOLEAN;
    if (type.getKind() == TypeKind.DOUBLE) return TypeName.DOUBLE;
    if (type.getKind() == TypeKind.FLOAT) return TypeName.FLOAT;
    if (type.getKind() == TypeKind.SHORT) return TypeName.SHORT;
    if (isOptionalType(type)) {
      TypeMirror inner = getOptionalInnerType(type);
      return ParameterizedTypeName.get(ClassName.get(Optional.class), resolveTypeName(inner).box());
    }
    // For declared types (String, Integer, etc.)
    return TypeName.get(type);
  }

  /**
   * Returns a Java expression that calls the appropriate {@code row.getXxx(colName)} method.
   *
   * <p>Uses {@code row.getValue()} as a fallback for unmapped types.
   */
  private String rowGetterCall(TypeMirror type, String colName) {
    String quoted = "\"" + colName + "\"";
    if (type.getKind() == TypeKind.LONG) return "row.getLong(" + quoted + ")";
    if (type.getKind() == TypeKind.INT) return "row.getInteger(" + quoted + ")";
    if (type.getKind() == TypeKind.BOOLEAN) return "row.getBoolean(" + quoted + ")";
    if (type.getKind() == TypeKind.DOUBLE) return "row.getDouble(" + quoted + ")";
    if (type.getKind() == TypeKind.FLOAT) return "row.getFloat(" + quoted + ")";
    if (type.getKind() == TypeKind.SHORT) return "row.getShort(" + quoted + ")";
    String typeName = type.toString();
    return switch (typeName) {
      case "java.lang.String" -> "row.getString(" + quoted + ")";
      case "java.lang.Long" -> "row.getLong(" + quoted + ")";
      case "java.lang.Integer" -> "row.getInteger(" + quoted + ")";
      case "java.lang.Boolean" -> "row.getBoolean(" + quoted + ")";
      case "java.lang.Double" -> "row.getDouble(" + quoted + ")";
      case "java.lang.Float" -> "row.getFloat(" + quoted + ")";
      case "java.lang.Short" -> "row.getShort(" + quoted + ")";
      case "java.time.LocalDate" -> "row.getLocalDate(" + quoted + ")";
      case "java.time.LocalDateTime" -> "row.getLocalDateTime(" + quoted + ")";
      case "java.time.OffsetDateTime" -> "row.getOffsetDateTime(" + quoted + ")";
      case "java.util.UUID" -> "row.getUUID(" + quoted + ")";
      default -> "row.getValue(" + quoted + ")";
    };
  }
}
