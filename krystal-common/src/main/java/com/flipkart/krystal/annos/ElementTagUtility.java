package com.flipkart.krystal.annos;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.core.ElementTagUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation is used to declare an {@link ElementTagUtils} implementation as THE utility for
 * handling operations on that annotation.
 */
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface ElementTagUtility {
  Class<? extends ElementTagUtils<?>> value();
}
