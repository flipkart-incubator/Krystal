package com.flipkart.krystal.annos;

import com.flipkart.krystal.core.KrystalElement.Vajram;
import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.Target;
import lombok.experimental.UtilityClass;

@ApplicableToElements(Vajram.class)
@Target({}) // App devs cannot use this in code. This is auto computed by the platform
public @interface OutputLogicDelegationMode {
  /**
   * Vajram developers are expected to leave this field empty and let the SDK infer the value from
   * the code. Else an error is thrown
   *
   * @return the type of delegation that this vajram's output logic uses
   */
  ComputeDelegationMode value();

  @UtilityClass
  final class Creator {
    public static @AutoAnnotation OutputLogicDelegationMode create(ComputeDelegationMode value) {
      return new AutoAnnotation_OutputLogicDelegationMode_Creator_create(value);
    }
  }
}
