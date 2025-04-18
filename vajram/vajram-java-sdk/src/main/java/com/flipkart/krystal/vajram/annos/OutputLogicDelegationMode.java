package com.flipkart.krystal.vajram.annos;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Vajram;
import com.flipkart.krystal.vajram.ComputeDelegationMode;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.Target;

@ApplicableToElements(Vajram.class)
@Target({}) // App devs cannot use this in code. This is auto computed by the platform
public @interface OutputLogicDelegationMode {
  /**
   * A vajram which extends {@link ComputeVajramDef} gets the value {@link
   * ComputeDelegationMode#NONE} and a class which extends {@link IOVajramDef} gets the value {@link
   * ComputeDelegationMode#SYNC}
   *
   * <p>Vajram developers are expected to leave this field empty and let the SDK infer the value
   * from the code. Else an error is thrown
   *
   * @return the type of delegation that this vajram's output logic uses
   */
  ComputeDelegationMode value();

  final class Creator {

    public static @AutoAnnotation OutputLogicDelegationMode create(ComputeDelegationMode value) {
      return new AutoAnnotation_OutputLogicDelegationMode_Creator_create(value);
    }

    private Creator() {}
  }
}
