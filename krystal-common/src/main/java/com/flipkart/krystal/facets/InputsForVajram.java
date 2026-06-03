package com.flipkart.krystal.facets;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.facets.InputsForVajram.InputsForVajrams;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

/** Declares that an interface defines the inputs of a Vajram. */
@Target(TYPE)
@Repeatable(InputsForVajrams.class)
public @interface InputsForVajram {

  /** The id of the vajram which uses this interface as inputs definition. */
  String vajramId();

  /**
   * The parent package of the all the auto generated models - the models are generated in {@code
   * parentPackage() + ".models"} or one of its sub-packages. Ideally should be same as the package
   * of the vajram class
   */
  String parentPackage();

  @Target(TYPE)
  @interface InputsForVajrams {
    InputsForVajram[] value();
  }
}
