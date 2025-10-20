package com.flipkart.krystal.config;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.TestOnly;

@UtilityClass
public class PropertyNames {

  /**
   * DO NOT SET THIS SYSTEM PROPERTY IN PRODUCTION RUNTIMES! - ELSE NEW VERSIONS OF CODE CAN BREAK
   * BACKWARD COMPATIBILITY. TO BE USED IN TESTING CODE ONLY.
   *
   * <p>Set this system property to {@code true} or {@code "true"} to allow all vajrams to be
   * invoked from outside the krystal graph instead of the default behaviour of only allowing
   * vajrams tagged with @{@link InvocableOutsideGraph}(allow=true)
   */
  @TestOnly
  public static final String RISKY_OPEN_ALL_VAJRAMS_TO_EXTERNAL_INVOCATION_PROP_NAME =
      "krystal.krystex.risky.openAllVajramsToExternalInvocation";
}
