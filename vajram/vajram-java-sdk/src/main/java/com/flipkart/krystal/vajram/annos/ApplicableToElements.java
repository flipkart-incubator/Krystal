package com.flipkart.krystal.vajram.annos;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

import com.flipkart.krystal.core.KrystalElement;
import java.lang.annotation.Target;

@Target(ANNOTATION_TYPE)
public @interface ApplicableToElements {
  Class<? extends KrystalElement> value();
}
