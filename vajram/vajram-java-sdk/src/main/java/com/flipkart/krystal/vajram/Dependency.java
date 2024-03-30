package com.flipkart.krystal.vajram;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Dependency {

  /**
   * The vajram request class of the vajram on which this vajram depends.
   *
   * <p>This is useful if we want to keep out the dependency vajram class from this vajram's
   * classpath. This allows for better inter-vajram decoupling and allows changes in the runtime
   * such that the depdendency vajram can later be moved to different service/runtime accessible
   * over the network - and this can be done without changing the code of the vajram (because the
   * request class will always be accessible as part this vajram's classpath.)
   *
   * <p>Due to this reason, setting this should be preferred over setting {@link #onVajram()} which
   * tightly couples the classpaths of this vajram and the dependency
   */
  Class<? extends VajramRequest> withVajramReq() default VajramRequest.class;

  /**
   * The vajram class on which this vajram depends. This should only be used if the dependency
   * vajram is in the same buildable module as this vajram. In all other cases, please use {@link
   * #withVajramReq()}
   */
  Class<? extends Vajram> onVajram() default Vajram.class;

  boolean canFanout() default false;
}
