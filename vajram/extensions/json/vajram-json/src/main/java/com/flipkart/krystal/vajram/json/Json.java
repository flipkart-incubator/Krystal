package com.flipkart.krystal.vajram.json;

import static java.util.Objects.requireNonNullElse;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static tools.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonInclude.Value;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.model.array.FloatArray;
import com.flipkart.krystal.model.array.SimpleFloatArray;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.vajram.json.ErrableModule.ErrableFilter;
import com.flipkart.krystal.vajram.json.JsonConfig.Creator;
import com.flipkart.krystal.vajram.json.array.ByteArrays.ByteArrayDeserializer;
import com.flipkart.krystal.vajram.json.array.ByteArrays.ByteArraySerializer;
import com.flipkart.krystal.vajram.json.array.FloatArrays.FloatArrayDeserializer;
import com.flipkart.krystal.vajram.json.array.FloatArrays.FloatArraySerializer;
import com.flipkart.krystal.vajram.json.array.JsonByteArray;
import com.flipkart.krystal.vajram.json.serialized.JsonRepresentation;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.datatype.guava.GuavaModule;

public final class Json implements SerdeProtocol<JsonConfig, SerializableJsonModel> {

  public static final Json JSON = new Json();

  public static final JsonMapper MAPPER =
      JsonMapper.builder()
          .withConfigOverride(
              Errable.class,
              mutableConfigOverride ->
                  mutableConfigOverride.setInclude(
                      Value.construct(
                          Include.NON_ABSENT,
                          Include.NON_ABSENT,
                          ErrableFilter.class,
                          ErrableFilter.class)))
          .changeDefaultPropertyInclusion(inc -> inc.withValueInclusion(Include.NON_ABSENT))
          .disable(FAIL_ON_UNKNOWN_PROPERTIES)
          .disable(FAIL_ON_EMPTY_BEANS)
          .disable(WRITE_DATES_AS_TIMESTAMPS)
          .addModules(
              new GuavaModule(), primitiveArrayModule(), new EnumModelModule(), new ErrableModule())
          .build();

  public static final ObjectReader OBJECT_READER = MAPPER.reader();
  public static final ObjectWriter OBJECT_WRITER = MAPPER.writer();

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
      customConfig = Creator.createDefault();
    }
    try {
      @Nullable Object transformed = tryAsJsonModel(object, mapper);
      return switch (customConfig.serializeAs()) {
        case BYTE_ARRAY ->
            transformed instanceof SerializableJsonModel jsonModel
                ? jsonModel._serialize().readAllBytes()
                : OBJECT_WRITER.writeValueAsBytes(transformed);
        case STRING ->
            transformed instanceof SerializableJsonModel jsonModel
                ? jsonModel._serializedJson()._asString()
                : OBJECT_WRITER.writeValueAsString(transformed);
      };
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> @Nullable T deserialize(
      Object payload, Object typeInfo, @Nullable JsonConfig customConfig) {
    if (typeInfo instanceof Class<?> clazz) {
      return (@NonNull T) deserialize(payload, clazz, customConfig);
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
    return deserialize(
        payload,
        OBJECT_READER.forType(OBJECT_READER.getConfig().getTypeFactory().constructType(typeInfo)));
  }

  public <T> @Nullable T deserialize(
      @Nullable Object payload, TypeReference<T> typeInfo, @Nullable JsonConfig customConfig) {
    return deserialize(payload, OBJECT_READER.forType(typeInfo));
  }

  private static <T> @Nullable T deserialize(@Nullable Object payload, ObjectReader reader) {
    if (payload == null) {
      return null;
    }
    try {
      return JsonRepresentation.of(payload)._deserialize(reader);
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

  private static SimpleModule primitiveArrayModule() {
    SimpleModule primitiveArrayModule = new SimpleModule("KrystalPrimitiveArrayModule");

    primitiveArrayModule
        .addAbstractTypeMapping(ByteArray.class, JsonByteArray.class)
        .addSerializer(ByteArray.class, new ByteArraySerializer())
        .addDeserializer(JsonByteArray.class, new ByteArrayDeserializer());

    primitiveArrayModule
        .addAbstractTypeMapping(FloatArray.class, SimpleFloatArray.class)
        .addSerializer(FloatArray.class, new FloatArraySerializer())
        .<@Nullable SimpleFloatArray>addDeserializer(
            SimpleFloatArray.class, new FloatArrayDeserializer());

    return primitiveArrayModule;
  }

  private Json() {}
}
