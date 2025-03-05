package com.flipkart.krystal.vajram.annos;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.core.KrystalElement;
import com.flipkart.krystal.vajram.VajramTraitDef;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a vajram "conforms" to or "exhibits" a trait. This is analogous to inheritance in
 * OOP.
 */
@Retention(RUNTIME)
@Target(TYPE)
@ApplicableToElements(KrystalElement.Vajram.class)
public @interface ConformsToTrait {
  Class<? extends VajramTraitDef<?>> withDef();
}
