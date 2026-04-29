package com.flipkart.krystal.vajram.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.flipkart.krystal.model.EnumModel;
import java.io.IOException;

/**
 * A Jackson module that provides fallback deserialization for {@link EnumModel} enums. When an
 * unknown enum value is encountered in JSON, it is deserialized to the {@code UNKNOWN} constant
 * (which must be the first enum constant).
 */
final class EnumModelModule extends SimpleModule {

  EnumModelModule() {
    super("KrystalEnumModelModule");
    setDeserializerModifier(new EnumModelDeserializerModifier());
  }

  private static class EnumModelDeserializerModifier extends BeanDeserializerModifier {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public JsonDeserializer<?> modifyEnumDeserializer(
        DeserializationConfig config,
        JavaType type,
        BeanDescription beanDesc,
        JsonDeserializer<?> deserializer) {
      Class<?> rawClass = type.getRawClass();
      if (EnumModel.class.isAssignableFrom(rawClass) && rawClass.isEnum()) {
        return new EnumModelDeserializer(rawClass, deserializer);
      }
      return deserializer;
    }
  }

  private static class EnumModelDeserializer<E extends Enum<E> & EnumModel>
      extends JsonDeserializer<E> {

    private final Class<E> enumClass;
    private final JsonDeserializer<?> delegate;
    private final E unknownValue;

    @SuppressWarnings("unchecked")
    EnumModelDeserializer(Class<?> enumClass, JsonDeserializer<?> delegate) {
      this.enumClass = (Class<E>) enumClass;
      this.delegate = delegate;
      // UNKNOWN is guaranteed to be the first constant by validation
      this.unknownValue = this.enumClass.getEnumConstants()[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public E deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      try {
        E value = (E) delegate.deserialize(p, ctxt);
        return value != null ? value : unknownValue;
      } catch (Exception e) {
        return unknownValue;
      }
    }
  }
}
