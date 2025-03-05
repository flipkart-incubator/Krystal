package com.flipkart.krystal.vajram.annos;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.core.KrystalElement;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a facet of a vajram "conforms" to a facet of the trait that the vajram conforms
 * to. This is analogous to @{@link Override} in Java.
 */
@Retention(RUNTIME)
@Target(TYPE)
@ApplicableToElements(KrystalElement.Vajram.class)
public @interface ConformsToFacet {
  int facetId();
}
