package com.flipkart.krystal.serial;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.model.array.ByteArray;
import java.lang.annotation.Annotation;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a serialization protocol used to serialize and deserialize facets values.
 *
 * @param <A> the type of the annotation which represents custom config for the serde protocol.
 */
public interface SerdeProtocol<A extends Annotation, T extends SerializableModel>
    extends ModelProtocol {

  /**
   * Returns the content-type which represents this serde protocol. This generally used as values of
   * "Accept" request header and "Content-Type" response header in client-server communication
   * protocols.
   */
  String defaultContentType();

  default @Nullable Object serialize(Object object) {
    return serialize(object, null);
  }

  default @Nullable Object serialize(Object object, @Nullable A customConfig) {
    return serialize(
        object,
        model -> {
          throw new UnsupportedOperationException();
        },
        customConfig);
  }

  /**
   * Serializes the given object using the given custom config if possible. Else throws a runtime
   * exception.
   *
   * @param object the object to serialize
   * @param modelMapper
   * @param customConfig custom configuration on how to serialize
   * @return the serialized value. The type of the return value depends on the serde protocol and
   *     the custom config. Examples: {@link ByteArray}, {@link String}
   */
  @Nullable Object serialize(
      Object object, Function<Model, T> modelMapper, @Nullable A customConfig);

  /**
   * Deserializes the given payload using the given type info and custom config if possible. Else
   * throws a runtime exception.
   *
   * @param payload the payload to deserialize - is generally a byte[] or sometimes a String
   * @param typeInfo the type to deserialize to
   * @param customConfig custom configuration on how to deserialize
   * @return the deserialized object
   */
  <T> @Nullable T deserialize(@Nullable Object payload, Object typeInfo, @Nullable A customConfig);
}
