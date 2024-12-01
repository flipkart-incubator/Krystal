package com.flipkart.krystal.tags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An instance of this annotation is used to assign tags dynamically to krystal graph elements. An
 * element can be dynamically assigned multiple tags of this annotation type, but no two of * these
 * can have the same {@link #name()}.
 *
 * <p>Generally elements in the krystal graph like logics, facets and vajrams are tagged using
 * custom annotations present in the java codebase. This annotation has been created specifically to
 * support scenarios where tagging needs to be done dynamically (via a UI for example). Application
 * developers and framework developers are encouraged to use custom annotations to annotate elements
 * in code (This is the reason this annotaiton has no @{@link Target}s specified).
 *
 * <p>An instance of this annotation can be created using the {@link
 * ElementTags#namedValueTag(String, String)} method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({}) // Not supported in source code. Use custom annotations instead
public @interface NamedValueTag {
  String name();

  String value();
}
