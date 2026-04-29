package com.flipkart.krystal.vajram.json;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.model.array.SimpleByteArray;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.vajram.json.array.ByteArrays.ByteArrayDeserializer;
import com.flipkart.krystal.vajram.json.array.ByteArrays.ByteArraySerializer;

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
          .registerModule(new Jdk8Module())
          .registerModule(new ParameterNamesModule())
          .registerModule(byteArrayModule())
          .registerModule(new EnumModelModule());

  public static final ObjectReader OBJECT_READER = OBJECT_MAPPER.reader();
  public static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writer();

  @Override
  public String modelClassesSuffix() {
    return "Json";
  }

  @Override
  public String defaultContentType() {
    return "application/json";
  }

  private static SimpleModule byteArrayModule() {
    return new SimpleModule("KrystalByteArrayModule")
        .addAbstractTypeMapping(ByteArray.class, SimpleByteArray.class)
        .addSerializer(SimpleByteArray.class, new ByteArraySerializer())
        .addDeserializer(SimpleByteArray.class, new ByteArrayDeserializer());
  }

  private Json() {}
}
