package com.flipkart.krystal.codegen.common.datatypes;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeGenerationException;
import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.model.array.SimpleByteArray;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.squareup.javapoet.CodeBlock;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
public enum StandardJavaType implements CodeGenType {
  BOOLEAN(
      TypeKind.BOOLEAN,
      CodeBlock.of("false"),
      boolean.class.getCanonicalName(),
      Boolean.class.getCanonicalName()),
  INT(
      TypeKind.INT,
      CodeBlock.of("0"),
      int.class.getCanonicalName(),
      Integer.class.getCanonicalName()),
  BYTE(
      TypeKind.BYTE,
      CodeBlock.of("0"),
      byte.class.getCanonicalName(),
      Byte.class.getCanonicalName()),
  SHORT(
      TypeKind.SHORT,
      CodeBlock.of("0"),
      short.class.getCanonicalName(),
      Short.class.getCanonicalName()),
  LONG(
      TypeKind.LONG,
      CodeBlock.of("0L"),
      long.class.getCanonicalName(),
      Long.class.getCanonicalName()),
  CHAR(
      TypeKind.CHAR,
      CodeBlock.of("0"),
      char.class.getCanonicalName(),
      Character.class.getCanonicalName()),
  FLOAT(
      TypeKind.FLOAT,
      CodeBlock.of("0.0f"),
      float.class.getCanonicalName(),
      Float.class.getCanonicalName()),
  DOUBLE(
      TypeKind.DOUBLE,
      CodeBlock.of("0.0d"),
      double.class.getCanonicalName(),
      Double.class.getCanonicalName()),
  VOID(
      CodeBlock.of("null"),
      processingEnv -> processingEnv.getTypeUtils().getNoType(TypeKind.VOID),
      void.class.getCanonicalName(),
      Void.class.getCanonicalName()),
  STRING(TypeKind.DECLARED, CodeBlock.of("$S", ""), String.class.getCanonicalName()),
  LIST_RAW(TypeKind.DECLARED, CodeBlock.of("$T.of()", List.class), List.class.getCanonicalName()),
  MAP_RAW(TypeKind.DECLARED, CodeBlock.of("$T.of()", Map.class), Map.class.getCanonicalName()),
  RANGE_RAW(
      TypeKind.DECLARED,
      // There is no sensible default for range because Range class has no none() static factory
      // method. Using Range.all() as default can have surprising
      // consequences for devs - so we force them to specify their own default as needed.
      null,
      Range.class.getCanonicalName()),
  BYTE_ARRAY(
      TypeKind.DECLARED,
      CodeBlock.of("$T.of()", SimpleByteArray.class),
      ByteArray.class.getCanonicalName()),
  // ...
  // Other Primitive Arrays to be added here
  // ...

  /* *** java time types ***/
  LOCAL_DATE_TIME(
      TypeKind.DECLARED,
      CodeBlock.of("$T.parse(\"1970-01-01T00:00:00\")", LocalDateTime.class),
      LocalDateTime.class.getCanonicalName()),
  OFFSET_DATE_TIME(
      TypeKind.DECLARED,
      CodeBlock.of("$T.parse(\"1970-01-01T00:00:00+00:00\")", OffsetDateTime.class),
      OffsetDateTime.class.getCanonicalName()),
  INSTANT(
      TypeKind.DECLARED,
      CodeBlock.of("$T.parse(\"1970-01-01T00:00:00Z\")", Instant.class),
      Instant.class.getCanonicalName()),
  LOCAL_DATE(
      TypeKind.DECLARED,
      CodeBlock.of("$T.parse(\"1970-01-01\")", LocalDate.class),
      LocalDate.class.getCanonicalName()),

  /* ** networking types ***/
  URL(TypeKind.DECLARED, null, URL.class.getCanonicalName());

  static final ImmutableMap<String, StandardJavaType> standardTypesByCanonicalName;

  static {
    Map<String, StandardJavaType> collector = new LinkedHashMap<>();
    for (StandardJavaType standardJavaType : StandardJavaType.values()) {
      for (String canonicalClassName : standardJavaType.canonicalClassNames()) {
        collector.put(canonicalClassName, standardJavaType);
      }
    }
    standardTypesByCanonicalName = ImmutableMap.copyOf(collector);
  }

  @SuppressWarnings("ImmutableEnumChecker")
  private final @Nullable CodeBlock defaultValueExpr;

  @SuppressWarnings("ImmutableEnumChecker")
  private final Function<ProcessingEnvironment, TypeMirror> getJavaModelType;

  private final String defaultCanonicalClassName;
  private final ImmutableList<String> canonicalClassNames;

  StandardJavaType(
      TypeKind typeKind,
      @Nullable CodeBlock defaultValueExpr,
      String defaultCanonicalClassName,
      String... canonicalClassNames) {
    this(
        defaultValueExpr,
        processingEnv -> {
          if (typeKind.isPrimitive()) {
            return processingEnv.getTypeUtils().getPrimitiveType(typeKind);
          }
          return processingEnv
              .getTypeUtils()
              .erasure(
                  requireNonNull(
                          processingEnv.getElementUtils().getTypeElement(defaultCanonicalClassName))
                      .asType());
        },
        defaultCanonicalClassName,
        canonicalClassNames);
  }

  StandardJavaType(
      @Nullable CodeBlock defaultValueExpr,
      Function<ProcessingEnvironment, TypeMirror> getJavaModelType,
      String defaultCanonicalClassName,
      String... otherCanonicalClassNames) {
    this.defaultValueExpr = defaultValueExpr;
    this.getJavaModelType = getJavaModelType;
    this.defaultCanonicalClassName = defaultCanonicalClassName;
    this.canonicalClassNames =
        ImmutableList.<String>builder()
            .add(defaultCanonicalClassName)
            .add(otherCanonicalClassNames)
            .build();
  }

  @Override
  public String canonicalClassName() {
    return defaultCanonicalClassName;
  }

  @Override
  public TypeMirror typeMirror(ProcessingEnvironment processingEnv) {
    return getJavaModelType.apply(processingEnv);
  }

  @Override
  public ImmutableList<CodeGenType> typeParameters() {
    return ImmutableList.of();
  }

  @Override
  public CodeBlock defaultValueExpr(ProcessingEnvironment processingEnv) {
    if (defaultValueExpr == null) {
      throw new CodeGenerationException(
          "No default value expression available for type '%s'".formatted(this));
    }
    return defaultValueExpr;
  }

  public static @Nullable StandardJavaType fromCanonicalClassName(String canonicalClassName) {
    return standardTypesByCanonicalName.get(canonicalClassName);
  }
}
