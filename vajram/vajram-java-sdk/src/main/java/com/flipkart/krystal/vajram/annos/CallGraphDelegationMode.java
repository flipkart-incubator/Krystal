package com.flipkart.krystal.vajram.annos;

import com.flipkart.krystal.vajram.ComputeDelegationMode;
import com.flipkart.krystal.vajram.KrystalElement.Vajram;
import java.lang.annotation.Documented;
import java.lang.annotation.Target;

@Documented
@Transitive
@ApplicableToElements(Vajram.class)
@Target({}) // App devs cannot use this in code. This is auto computed by the platform
public @interface CallGraphDelegationMode {
  ComputeDelegationMode value();
}
