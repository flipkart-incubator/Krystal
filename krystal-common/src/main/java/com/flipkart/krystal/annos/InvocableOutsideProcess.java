package com.flipkart.krystal.annos;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.core.KrystalElement.Vajram;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
@ApplicableToElements(Vajram.class)
public @interface InvocableOutsideProcess {}
