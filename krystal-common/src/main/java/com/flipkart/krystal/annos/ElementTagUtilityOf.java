package com.flipkart.krystal.annos;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.core.ElementTagUtils;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark a class implementing {@link ElementTagUtils} as an
 * ElementTagUtility for a specific annotation. This is an inverse annotation to @{@link
 * ElementTagUtility} and is designed to prevent human errors where an annotation is tagged to an
 * incorrect implementation of {@link ElementTagUtils}
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface ElementTagUtilityOf {
  Class<? extends Annotation> value();
}
