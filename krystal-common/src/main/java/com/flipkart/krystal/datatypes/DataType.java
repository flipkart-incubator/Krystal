package com.flipkart.krystal.datatypes;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Type;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;

public sealed interface DataType<T> permits JavaType {

  /**
   * Returns the java {@link Type} corresponding to this data type.
   *
   * @throws ClassNotFoundException if the reflection type is not present, or not available to the
   *     current {@link ClassLoader}
   */
  Type javaReflectType() throws ClassNotFoundException;

  TypeMirror javaModelType(ProcessingEnvironment processingEnv);

  String canonicalClassName();

  /**
   * Returns the default value for this data type. This is useful in case the developer has marked
   * the datatype as mandatory but there is no way to detect if the value present or not - forcing
   * the platform to choose a default.
   *
   * <p>The most common example for this is scalars (like primitive types) . This value becomes
   * especially relevant in distributed environments when the client vajram is in a different
   * process and the server vajram is in a different process. In the case of non scalar types, if
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
   * never fail mandatory checks in the case of serialized communtation over the wire between
   * vajrams in a distributed setup.
   *
   * <p>To make this behaviour choice explicit (whether we differentate between set and unset
   * values), Vajram developers can opt into this memory saving behaviour by tagging the facet with
   * `@Mandatory(ifNotSet = DEFAULT_TO_ZERO)`, `@Mandatory(ifNotSet = DEFAULT_TO_EMPTY)`
   * or @Mandatory(ifNotSet = DEFAULT_TO_FALSE) as the case may be. In case of lists and maps, many
   * serialization protocols don't provide a way to differentiate between set and unset. So tagging
   * facets of list and map types with `@Mandatory(ifNotSet = DEFAULT_TO_EMPTY)` may not be allowed
   * when the vajram is configured to be serialized over such protocols - and the SDK might throw a
   * compilation error upfront. When the developer does opt-in to this optimization via
   * DEFAULT_TO_..., the relevant platform default value is always used in case the value is not
   * set.
   *
   * <p>This method returns that default value.
   *
   * @throws ClassNotFoundException if {@link #javaReflectType()} throws {@link
   *     ClassNotFoundException}
   * @throws IllegalArgumentException if the datatype does not have a platform default value.
   */
  @NonNull T getPlatformDefaultValue() throws ClassNotFoundException, IllegalArgumentException;

  /**
   * Returns true if the datatype has a platform default value. Generally true for scalars,
   * collections/arrays, maps and strings.
   */
  boolean hasPlatformDefaultValue(ProcessingEnvironment processingEnv);

  /**
   * Returns the raw type of this data type. For example, if this represents a {@link List}<{@link
   * String}>,this will return {@link List}
   */
  DataType<T> rawType();

  ImmutableList<DataType<?>> typeParameters();
}
