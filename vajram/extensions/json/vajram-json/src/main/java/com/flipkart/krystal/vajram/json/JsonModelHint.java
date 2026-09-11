package com.flipkart.krystal.vajram.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker used alongside {@code @JsonDeserialize(using = ModelErrableDeserializer.class)} / {@code
 * contentUsing = ModelErrableDeserializer.class} to tell {@link ModelErrableDeserializer} which
 * concrete {@code _ImmutJson} class to deserialize the {@code Errable}'s wrapped Model value as.
 *
 * <p>This indirection is needed because {@code @JsonDeserialize(as=...)}/{@code contentAs=...} can
 * only narrow a type to a subtype of itself, and {@code ImmutJson} is not a subtype of {@code
 * Errable<Model>} - only of {@code Model} itself.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonModelHint {
  Class<?> value();
}
