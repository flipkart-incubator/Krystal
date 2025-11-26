package com.flipkart.krystal.codegen.common.datatypes;

import com.flipkart.krystal.codegen.common.models.CodeGenerationException;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public interface CodeGenType {

  String canonicalClassName();

  ImmutableList<CodeGenType> typeParameters();

  TypeMirror javaModelType(ProcessingEnvironment processingEnv);

  /**
   * Returns the raw type of this data type. For example, if this represents a {@link List}<{@link
   * String}>,this will return {@link List}.
   *
   * <p>If the data type is a type variable, this will return the raw type of the upperBound. For
   * example, if this represents a type variable T extends {@link List}< U extends {@link String}>,
   * this will return {@link List}<{@link String}>.
   */
  CodeGenType rawType();

  /**
   * Returns the default value for this data type. This is useful in case the developer has marked
   * the datatype as mandatory but there is no way to detect if the value present or not - forcing
   * the platform to choose a default.
   *
   * <p>The most common example for this is scalars (like primitive types) . This value becomes
   * especially relevant in distributed environments when the client vajram is in a different
   * process and the server vajram is in a different process. In the case of non-scalar types, if
   * the client vajram does not pass any value for the mandatory facet, we check for null and throw
   * an error. But in the case of scalar values, there is no way to check if the client has sent the
   * value or not. This is because of quirks of serialization protocols. Wire protocols like json,
   * <a href="https://github.com/google/flatbuffers/issues/6014">flatbuffers</a> and <a
   * href="https://github.com/protocolbuffers/protobuf/blob/main/docs/field_presence.md">protobuf
   * (version 2 and version 3.15+) </a> allow marking a scalar field as optional. Similarly in
   * in-memory objects,we can potentially detect missing primitive values by modelling the field as
   * a boxed primitive and checking its value for null. But this optionality comes at the cost of
   * suboptimal memory utilization (This is because of the extra bytes used to encode the null case
   * and the additional pointer hop to access the value. While this might be ignorable in some
   * cases, the savings might be significant for serialized messages and in-memory java objects in
   * low-bandwidth or high-performance environments).
   *
   * <p>This is the reason wire protocols like protobuf and flatbuffers provide a way to mark scalar
   * and list/map types as non-optional and they auto-assign zero-ish/empty values to the scalars
   * and collections when they are non-optional, since in these optimized cases we cannot
   * differentiate between the field being set or not set (default values are not transmitted over
   * the wire). This means non-optional scalars and collections will never be null and hence can
   * never fail mandatory checks in the case of serialized communication over the wire between
   * vajrams in a distributed setup.
   *
   * <p>To make this behaviour choice explicit (whether we differentiate between set and unset
   * values), Vajram developers can opt into this memory saving behaviour by tagging the facet with
   * `@IfAbsent(ASSUME_DEFAULT_VALUE)`. In case of lists and maps, many serialization protocols
   * don't provide a way to differentiate between value present and absent. So tagging facets of
   * list and map types with `@IfAbsent(FAIL)` and `@IfAbsent(MAY_FAIL_CONDITIONALLY)` may not be
   * allowed when the vajram is configured to be serialized over such protocols - and the SDK might
   * throw a compilation error upfront. When the developer does opt-in to this optimization via
   * DEFAULT_TO_..., the relevant platform default value is always used in case the value is not
   * set.
   *
   * <p>This method returns that default value.
   *
   * @throws CodeGenerationException if the datatype does not have a platform default value.
   */
  CodeBlock defaultValueExpr(ProcessingEnvironment processingEnv) throws CodeGenerationException;
}
