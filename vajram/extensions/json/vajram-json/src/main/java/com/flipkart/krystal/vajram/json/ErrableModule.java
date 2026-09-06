package com.flipkart.krystal.vajram.json;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.NonNil;
import org.checkerframework.checker.nullness.qual.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * A Jackson module that provides serialization and deserialization support for {@link Errable}.
 *
 * <ul>
 *   <li><b>Serialization:</b> {@link NonNil} → the inner value; {@code Nil} or {@code Failure} →
 *       {@code null} (absent JSON field when the mapper is configured with {@code NON_ABSENT}
 *       inclusion).
 *   <li><b>Deserialization:</b> JSON value → {@code Errable.withValue(value)}; JSON {@code null} →
 *       {@code Errable.nil()}.
 * </ul>
 *
 * <p>Note: {@code Failure} state cannot be round-tripped through JSON — it is always serialized as
 * absent/null and re-deserialized as {@code Nil}.
 */
final class ErrableModule extends SimpleModule {

  @SuppressWarnings("rawtypes")
  ErrableModule() {
    super("KrystalErrableModule");
    addSerializer(Errable.class, new ErrableSerializer());
    addDeserializer(Errable.class, new ErrableDeserializer(null));
  }

  /**
   * Serializes {@code Errable<T>}: writes the inner value for {@link NonNil}, {@code null}
   * otherwise.
   */
  @SuppressWarnings("rawtypes")
  private static final class ErrableSerializer extends StdSerializer<Errable> {

    @SuppressWarnings("unchecked")
    ErrableSerializer() {
      super(Errable.class);
    }

    @Override
    public void serialize(Errable value, JsonGenerator gen, SerializationContext context) {
      if (value instanceof NonNil<?> nonNil) {
        Object nonNilValue = nonNil.value();
        context
            .findRootValueSerializer(nonNilValue.getClass())
            .serialize(nonNilValue, gen, context);
      } else {
        context.findNullValueSerializer(null).serialize(null, gen, context);
      }
    }

    @Override
    public boolean isEmpty(SerializationContext provider, Errable value) {
      // Nil and Failure are treated as absent (no JSON field emitted when NON_ABSENT is configured)
      return !(value instanceof NonNil<?>);
    }
  }

  /**
   * Deserializes {@code Errable<T>}: reads the inner type T using a contextually resolved
   * deserializer and wraps it in {@link Errable#withValue}.
   */
  @SuppressWarnings("rawtypes")
  private static final class ErrableDeserializer extends StdDeserializer<Errable> {

    private final @Nullable ValueDeserializer<Object> innerDeserializer;

    ErrableDeserializer(@Nullable ValueDeserializer<Object> innerDeserializer) {
      super(Errable.class);
      this.innerDeserializer = innerDeserializer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Errable<?> deserialize(JsonParser p, DeserializationContext ctx) {
      Object value =
          innerDeserializer != null
              ? innerDeserializer.deserialize(p, ctx)
              : p.readValueAs(Object.class);
      return Errable.withValue(value);
    }

    @Override
    public Errable<?> getNullValue(DeserializationContext ctx) {
      return Errable.nil();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValueDeserializer<?> createContextual(
        DeserializationContext context, BeanProperty property) {
      JavaType type = context.getContextualType();
      if (type == null && property != null) {
        type = property.getType();
      }
      if (type != null && type.hasGenericTypes()) {
        JavaType innerType = type.containedType(0);
        ValueDeserializer<Object> inner =
            context.findContextualValueDeserializer(innerType, property);
        return new ErrableDeserializer(inner);
      }
      return this;
    }
  }

  public static final class ErrableFilter {

    /**
     * Return true if object is to be excluded. So return true for nulls and empty
     *
     * @param obj the reference object with which to compare.
     */
    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return true;
      }
      if (obj instanceof Errable<?> errable) {
        return errable.value() == null;
      }
      return false;
    }
  }
}
