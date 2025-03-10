package com.flipkart.krystal.annos;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** An annotation which has this annotation must have a pub */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
public @interface HasCreator {}
