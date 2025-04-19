package com.flipkart.krystal.vajram.annos;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Vajram;
import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import lombok.experimental.UtilityClass;

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

  @UtilityClass
  final class Creator {
    public static @AutoAnnotation VajramIdentifier create(String value) {
      return new AutoAnnotation_VajramIdentifier_Creator_create(value);
    }
  }
}
