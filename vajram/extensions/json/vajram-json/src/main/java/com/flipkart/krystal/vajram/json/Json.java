package com.flipkart.krystal.vajram.json;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.util.Objects.requireNonNullElse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.vajram.json.array.ByteArrays.ByteArrayDeserializer;
import com.flipkart.krystal.vajram.json.array.ByteArrays.ByteArraySerializer;
import com.flipkart.krystal.vajram.json.array.JsonByteArray;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Json implements SerdeProtocol<JsonConfig, SerializableJsonModel> {

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

  @Override
  public Object serialize(
      Object object,
      Function<Model, @Nullable SerializableJsonModel> mapper,
      @Nullable JsonConfig customConfig) {
    if (customConfig == null) {
      customConfig = JsonConfig.Creator.createDefault();
    }
    try {
      object = tryAsJsonModel(object, mapper);
      return switch (customConfig.serializeAs()) {
        case BYTE_ARRAY ->
            object instanceof SerializableJsonModel jsonModel
                ? jsonModel._serialize()
                : OBJECT_WRITER.writeValueAsBytes(object);
        case STRING ->
            object instanceof SerializableJsonModel jsonModel
                ? jsonModel._serializeAsString()
                : OBJECT_WRITER.writeValueAsString(object);
      };
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static Object tryAsJsonModel(
      Object object, Function<Model, @Nullable SerializableJsonModel> mapper) {
    if (object instanceof Model model) {
      object =
          requireNonNullElse(
              asJsonModel(model, mapper),
              // if the model is cannot be converted to SerializableJsonModel,
              // return the model itself - maybe there is a custom serializer
              model);
    } else if (object instanceof List<?> list) {
      object = Lists.transform(list, input -> tryAsJsonModel(input, mapper));
    } else if (object instanceof Map<?, ?> map) {
      object = Maps.transformValues(map, input -> tryAsJsonModel(input, mapper));
    }
    return object;
  }

  private static @Nullable SerializableJsonModel asJsonModel(
      Model model, Function<Model, @Nullable SerializableJsonModel> mapper) {
    if (model instanceof SerializableJsonModel serializableJsonModel) {
      return serializableJsonModel;
    } else {
      return mapper.apply(model);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T deserialize(Object payload, Object typeInfo, @Nullable JsonConfig customConfig) {
    if (typeInfo instanceof Type type) {
      return deserialize(payload, type, customConfig);
    } else if (typeInfo instanceof TypeReference<?> typeRef) {
      return deserialize(payload, (TypeReference<? extends T>) typeRef, customConfig);
    } else {
      throw new IllegalArgumentException("Unsupported typeInfo: " + typeInfo);
    }
  }

  public <T> T deserialize(
      Object payload, Class<? extends T> typeInfo, @Nullable JsonConfig customConfig) {
    try {
      ObjectReader reader = OBJECT_READER.forType(typeInfo);
      T deserializedValue = readValue(payload, reader);
      if (deserializedValue != null) {
        return deserializedValue;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass());
  }

  public <T> T deserialize(Object payload, Type typeInfo, @Nullable JsonConfig customConfig) {
    try {
      ObjectReader reader = OBJECT_READER.forType(typeInfo);
      T deserializedValue = readValue(payload, reader);
      if (deserializedValue != null) {
        return deserializedValue;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass());
  }

  public <T> T deserialize(
      Object payload, TypeReference<T> typeInfo, @Nullable JsonConfig customConfig) {
    try {
      ObjectReader reader = OBJECT_READER.forType(typeInfo);
      T deserializedValue = readValue(payload, reader);
      if (deserializedValue != null) {
        return deserializedValue;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass());
  }

  private static SimpleModule byteArrayModule() {
    return new SimpleModule("KrystalByteArrayModule")
        .addAbstractTypeMapping(ByteArray.class, JsonByteArray.class)
        .addSerializer(ByteArray.class, new ByteArraySerializer())
        .addDeserializer(JsonByteArray.class, new ByteArrayDeserializer());
  }

  private static <T> @Nullable T readValue(Object payload, ObjectReader reader) throws IOException {
    if (payload instanceof JsonByteArray jsonByteArray) {
      return jsonByteArray.readFromJson(reader);
    } else if (payload instanceof byte[] bytes) {
      return reader.readValue(bytes);
    } else if (payload instanceof String string) {
      return reader.readValue(string);
    }
    return null;
  }

  private Json() {}
}
