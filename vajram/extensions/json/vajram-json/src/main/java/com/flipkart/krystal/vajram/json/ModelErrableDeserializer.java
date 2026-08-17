package com.flipkart.krystal.vajram.json;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.flipkart.krystal.data.Errable;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Deserializes {@code Errable<Model>}-shaped values, including when the {@code Model} is nested
 * inside a {@code List}/{@code Map} that the {@code Errable} wraps (e.g. {@code
 * Errable<List<Model>>}), or when the {@code Model} itself is wrapped per-element inside a {@code
 * List}/{@code Map} (e.g. {@code List<Errable<Model>>}).
 *
 * <p>Used (via {@code @JsonDeserialize(using=...)}/{@code contentUsing=...}) together with {@link
 * JsonModelHint}, which supplies the concrete {@code _ImmutJson} class to resolve the wrapped
 * {@code Model} value as - since {@code contentAs}/{@code as} alone can only narrow a type to a
 * subtype of itself, and {@code ImmutJson} is a subtype of {@code Model}, not of {@code
 * Errable<Model>} (or a {@code List}/{@code Map} thereof).
 */
@SuppressWarnings("rawtypes")
public final class ModelErrableDeserializer extends StdDeserializer<Errable>
    implements ContextualDeserializer {

  private final @Nullable JsonDeserializer<Object> innerDeserializer;

  public ModelErrableDeserializer() {
    this(null);
  }

  private ModelErrableDeserializer(@Nullable JsonDeserializer<Object> innerDeserializer) {
    super(Errable.class);
    this.innerDeserializer = innerDeserializer;
  }

  @Override
  public Errable<?> deserialize(com.fasterxml.jackson.core.JsonParser p, DeserializationContext ctx)
      throws IOException {
    if (innerDeserializer == null) {
      // Not contextualized (missing @JsonModelHint) - fall back to generic Object resolution.
      return Errable.withValue(p.readValueAs(Object.class));
    }
    return Errable.withValue(innerDeserializer.deserialize(p, ctx));
  }

  @Override
  public Errable<?> getNullValue(DeserializationContext ctx) {
    return Errable.nil();
  }

  @Override
  public JsonDeserializer<?> createContextual(
      DeserializationContext ctx, @Nullable BeanProperty property) throws JsonMappingException {
    if (property == null) {
      return this;
    }
    JsonModelHint hint = property.getAnnotation(JsonModelHint.class);
    if (hint == null) {
      return this;
    }
    JavaType modelType = ctx.getTypeFactory().constructType(hint.value());
    // The type of the value wrapped by this Errable: either the Model itself (scalar), or a
    // List/Map of it (when the whole List/Map - not just its elements - is Errable-wrapped).
    JavaType wrappedType = ctx.getContextualType();
    JavaType innerBase =
        wrappedType != null && wrappedType.hasGenericTypes() ? wrappedType.containedType(0) : null;
    JavaType targetInner;
    if (innerBase != null && innerBase.isCollectionLikeType()) {
      targetInner = innerBase.withContentType(narrowedContent(ctx, innerBase, modelType));
    } else if (innerBase != null && innerBase.isMapLikeType()) {
      targetInner = innerBase.withContentType(narrowedContent(ctx, innerBase, modelType));
    } else {
      targetInner = modelType;
    }
    JsonDeserializer<Object> inner = ctx.findContextualValueDeserializer(targetInner, property);
    return new ModelErrableDeserializer(inner);
  }

  /**
   * If the List/Map's declared content type is itself Errable (e.g. {@code List<Errable<Model>>}
   * nested inside {@code Errable<List<Errable<Model>>>}), narrow to {@code Errable<ImmutJson>}
   * rather than bare {@code ImmutJson}, so the standard (globally-registered) Errable deserializer
   * - not this one - resolves each element.
   */
  private JavaType narrowedContent(
      DeserializationContext ctx, JavaType containerType, JavaType modelType) {
    JavaType declaredContent = containerType.getContentType();
    if (declaredContent != null && declaredContent.hasRawClass(Errable.class)) {
      return ctx.getTypeFactory().constructParametricType(Errable.class, modelType);
    }
    return modelType;
  }
}
