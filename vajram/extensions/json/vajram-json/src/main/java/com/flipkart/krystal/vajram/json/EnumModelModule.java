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
import com.flipkart.krystal.model.ModelRoot;

/**
 * A Jackson module that provides fallback deserialization for {@link EnumModel} enums. When an
 * unknown enum value is encountered in JSON, it is deserialized to the first enum constant. For
 * {@link EnumModel}s with @{@link ModelRoot} annotation, {@code UNKNOWN} is guaranteed to be the
 * first enum constant.
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

    private final JsonDeserializer<?> delegate;
    private final E firstEnumConstant;

    @SuppressWarnings("unchecked")
    EnumModelDeserializer(Class<? extends Enum> enumClass, JsonDeserializer<?> delegate) {
      this.delegate = delegate;
      this.firstEnumConstant = ((Class<E>) enumClass).getEnumConstants()[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public E deserialize(JsonParser p, DeserializationContext ctxt) {
      try {
        return (E) delegate.deserialize(p, ctxt);
      } catch (Exception e) {
        return firstEnumConstant;
      }
    }
  }
}
