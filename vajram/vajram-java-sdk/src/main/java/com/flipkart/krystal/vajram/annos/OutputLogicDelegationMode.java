package com.flipkart.krystal.vajram.annos;

import com.flipkart.krystal.vajram.ComputeDelegationMode;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.KrystalElement.Vajram;
import java.lang.annotation.Annotation;
import java.lang.annotation.Target;

@ApplicableToElements(Vajram.class)
@Target({}) // App devs cannot use this in code. This is auto computed by the platform
public @interface OutputLogicDelegationMode {
  /**
   * A vajram which extends {@link ComputeVajram} gets the value {@link
   * ComputeDelegationMode#NO_DELEGATION} and a class which extends {@link IOVajram} gets the value
   * {@link ComputeDelegationMode#SYNC_DELEGATION}
   *
   * <p>Vajram developers are expected to leave this field empty and let the SDK infer the value
   * from the code. Else an error is thrown
   *
   * @return the type of delegation that this vajram's output logic uses
   */
  ComputeDelegationMode value();

  public record OutputLogicDelegationModeImpl(ComputeDelegationMode value)
      implements OutputLogicDelegationMode {

    @Override
    public Class<? extends Annotation> annotationType() {
      return OutputLogicDelegationMode.class;
    }
  }
}
