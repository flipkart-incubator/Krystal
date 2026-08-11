package com.flipkart.krystal.vajram.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.NonNil;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;

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

    ErrableSerializer() {
      super(Errable.class);
    }

    @Override
    public void serialize(Errable value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      if (value instanceof NonNil<?> nonNil) {
        provider.defaultSerializeValue(nonNil.value(), gen);
      } else {
        gen.writeNull();
      }
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Errable value) {
      // Nil and Failure are treated as absent (no JSON field emitted when NON_ABSENT is configured)
      return !(value instanceof NonNil<?>);
    }
  }

  /**
   * Deserializes {@code Errable<T>}: reads the inner type T using a contextually resolved
   * deserializer and wraps it in {@link Errable#withValue}.
   */
  @SuppressWarnings("rawtypes")
  private static final class ErrableDeserializer extends StdDeserializer<Errable>
      implements ContextualDeserializer {

    private final @Nullable JsonDeserializer<Object> innerDeserializer;

    ErrableDeserializer(@Nullable JsonDeserializer<Object> innerDeserializer) {
      super(Errable.class);
      this.innerDeserializer = innerDeserializer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Errable<?> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
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
    public JsonDeserializer<?> createContextual(DeserializationContext ctx, BeanProperty property)
        throws JsonMappingException {
      JavaType type = ctx.getContextualType();
      if (type == null && property != null) {
        type = property.getType();
      }
      if (type != null && type.hasGenericTypes()) {
        JavaType innerType = type.containedType(0);
        JsonDeserializer<Object> inner = ctx.findContextualValueDeserializer(innerType, property);
        return new ErrableDeserializer(inner);
      }
      return this;
    }
  }
}
