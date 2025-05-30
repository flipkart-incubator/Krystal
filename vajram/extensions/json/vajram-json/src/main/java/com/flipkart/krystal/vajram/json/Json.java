package com.flipkart.krystal.vajram.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.serial.SerdeProtocol;

public final class Json implements SerdeProtocol {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .registerModule(new GuavaModule())
          .registerModule(new JavaTimeModule())
          .registerModule(new Jdk8Module());

  public static final ObjectReader OBJECT_READER = OBJECT_MAPPER.reader();
  public static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writer();
  public static final String JSON_SUFFIX = "Json";

  private Json() {}
}
