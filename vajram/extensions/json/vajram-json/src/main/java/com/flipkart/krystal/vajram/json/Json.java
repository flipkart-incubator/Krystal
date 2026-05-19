package com.flipkart.krystal.vajram.json;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.serial.SerdeProtocol;

public final class Json implements SerdeProtocol {

  public static final Json JSON = new Json();

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .setSerializationInclusion(NON_ABSENT)
          .disable(FAIL_ON_UNKNOWN_PROPERTIES)
          .disable(FAIL_ON_EMPTY_BEANS)
          .disable(WRITE_DATES_AS_TIMESTAMPS)
          .registerModule(new GuavaModule())
          .registerModule(new JavaTimeModule())
          .registerModule(new Jdk8Module());

  public static final ObjectReader OBJECT_READER = OBJECT_MAPPER.reader();
  public static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writer();

  /**
   * Converts a value to the specified type using the configured ObjectMapper.
   *
   * <p>This method respects {@code @JsonDeserialize} annotations and uses the configured modules and
   * settings. It's more efficient than serializing to bytes and deserializing.
   *
   * @param fromValue The value to convert (typically a Map)
   * @param toValueType The target type class
   * @param <T> The target type
   * @return The converted value
   * @throws IllegalArgumentException if conversion fails
   */
  public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
    return OBJECT_MAPPER.convertValue(fromValue, toValueType);
  }

  @Override
  public String modelClassesSuffix() {
    return "Json";
  }

  @Override
  public String defaultContentType() {
    return "application/json";
  }

  private Json() {}
}
