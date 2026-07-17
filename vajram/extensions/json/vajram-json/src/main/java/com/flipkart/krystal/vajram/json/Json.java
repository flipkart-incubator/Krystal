package com.flipkart.krystal.vajram.json;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.util.Objects.requireNonNullElse;

import com.fasterxml.jackson.annotation.JsonInclude.Value;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
import com.flipkart.krystal.vajram.json.serialized.SerializedJson;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

public final class Json implements SerdeProtocol<JsonConfig, SerializableJsonModel> {

  public static final Json JSON = new Json();

  private static final JsonMapper OBJECT_MAPPER =
      JsonMapper.builder()
          .defaultPropertyInclusion(Value.ALL_NON_ABSENT)
          .disable(FAIL_ON_UNKNOWN_PROPERTIES)
          .disable(FAIL_ON_EMPTY_BEANS)
          .disable(WRITE_DATES_AS_TIMESTAMPS)
          .addModules(
              new GuavaModule(),
              new JavaTimeModule(),
              new Jdk8Module(),
              new ParameterNamesModule(),
              byteArrayModule(),
              new EnumModelModule())
          .build();

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
  public @PolyNull Object serialize(
      @PolyNull Object object,
      Function<Model, @Nullable SerializableJsonModel> mapper,
      @Nullable JsonConfig customConfig) {
    if (object == null) {
      return null;
    }
    if (customConfig == null) {
      customConfig = JsonConfig.Creator.createDefault();
    }
    try {
      @Nullable Object transformed = tryAsJsonModel(object, mapper);
      return switch (customConfig.serializeAs()) {
        case BYTE_ARRAY ->
            transformed instanceof SerializableJsonModel jsonModel
                ? jsonModel._serialize()
                : OBJECT_WRITER.writeValueAsBytes(transformed);
        case STRING ->
            transformed instanceof SerializableJsonModel jsonModel
                ? jsonModel._serializedJson().asString()
                : OBJECT_WRITER.writeValueAsString(transformed);
      };
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> @Nullable T deserialize(
      @Nullable Object payload, Object typeInfo, @Nullable JsonConfig customConfig) {
    if (typeInfo instanceof Class<?> clazz) {
      return (T) deserialize(payload, clazz, customConfig);
    } else if (typeInfo instanceof Type type) {
      return deserialize(payload, type, customConfig);
    } else if (typeInfo instanceof TypeReference<?> typeRef) {
      return (T) deserialize(payload, typeRef, customConfig);
    } else {
      throw new IllegalArgumentException("Unsupported typeInfo: " + typeInfo);
    }
  }

  public <T> @Nullable T deserialize(
      @Nullable Object payload, Class<? extends T> typeInfo, @Nullable JsonConfig customConfig) {
    return deserialize(payload, OBJECT_READER.forType(typeInfo));
  }

  public <T> @Nullable T deserialize(
      @Nullable Object payload, Type typeInfo, @Nullable JsonConfig customConfig) {
    return deserialize(payload, OBJECT_READER.forType(typeInfo));
  }

  public <T> @Nullable T deserialize(
      @Nullable Object payload, TypeReference<T> typeInfo, @Nullable JsonConfig customConfig) {
    return deserialize(payload, OBJECT_READER.forType(typeInfo));
  }

  private static <T> @Nullable T deserialize(@Nullable Object payload, ObjectReader reader) {
    if (payload == null) {
      return null;
    }
    if (payload instanceof Optional<? extends @Nullable Object> optional) {
      @SuppressWarnings("argument")
      Object innerPayload = optional.orElse(null);
      return deserialize(innerPayload, reader);
    }
    try {
      return SerializedJson.of(payload).deserialize(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("type.argument")
  private @Nullable static Object tryAsJsonModel(
      @Nullable Object object, Function<Model, @Nullable SerializableJsonModel> mapper) {
    if (object == null) {
      return null;
    }
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

  private static SimpleModule byteArrayModule() {
    return new SimpleModule("KrystalByteArrayModule")
        .addAbstractTypeMapping(ByteArray.class, JsonByteArray.class)
        .addSerializer(ByteArray.class, new ByteArraySerializer())
        .addDeserializer(JsonByteArray.class, new ByteArrayDeserializer());
  }

  private Json() {}
}
