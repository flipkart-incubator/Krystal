package com.flipkart.krystal.vajram.annos;

import com.flipkart.krystal.vajram.KrystalElement.Vajram;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Target;

@ApplicableToElements(Vajram.class)
@Target({}) // App devs cannot use this in code. This is auto computed by the platform
@Documented
public @interface VajramIdentifier {
  /**
   * Currently custom vajramIds are not supported. If this value digresses from the VajramClass
   * name, then an exception is thrown while loading the vajram. Developers must skip this field and
   * let the SDK infer the vajramId from the class name.
   *
   * @return the id of this vajram
   */
  String value();

  public static record VajramIdentifierImpl(String value) implements VajramIdentifier {

    @Override
    public Class<? extends Annotation> annotationType() {
      return VajramIdentifier.class;
    }
  }
}
