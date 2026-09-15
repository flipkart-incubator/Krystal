package com.flipkart.krystal.vajram.json;

import com.flipkart.krystal.model.EnumModel;
import com.flipkart.krystal.model.ModelRoot;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.module.SimpleModule;

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

  private static class EnumModelDeserializerModifier extends ValueDeserializerModifier {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ValueDeserializer<?> modifyEnumDeserializer(
        DeserializationConfig config,
        JavaType type,
        BeanDescription.Supplier beanDescRef,
        ValueDeserializer<?> deserializer) {
      Class<?> rawClass = type.getRawClass();
      if (EnumModel.class.isAssignableFrom(rawClass) && rawClass.isEnum()) {
        return new EnumModelDeserializer(rawClass, deserializer);
      }
      return deserializer;
    }
  }

  private static class EnumModelDeserializer<E extends Enum<E> & EnumModel>
      extends ValueDeserializer<E> {

    private final ValueDeserializer<?> delegate;
    private final E firstEnumConstant;

    @SuppressWarnings("unchecked")
    EnumModelDeserializer(Class<? extends Enum> enumClass, ValueDeserializer<?> delegate) {
      this.delegate = delegate;
      E[] enumConstants = ((Class<E>) enumClass).getEnumConstants();
      if (enumConstants == null) {
        throw new IllegalArgumentException(
            "Expect enum class but received a non-enum class: " + enumClass);
      }
      if (enumConstants.length == 0) {
        throw new IllegalArgumentException(
            "Every enum class must have at least one enum constant - so that it can be used as the default value");
      }
      this.firstEnumConstant = enumConstants[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public E deserialize(JsonParser p, DeserializationContext context) {
      try {
        return (E) delegate.deserialize(p, context);
      } catch (Exception e) {
        return firstEnumConstant;
      }
    }
  }
}
