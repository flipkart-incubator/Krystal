package com.flipkart.krystal.serial;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.VajramRoot;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(SOURCE)
@Target(TYPE)
@ApplicableToElements(VajramRoot.class)
public @interface SupportedSerdeProtocols {
  /** The set of serialization protocols supported by the models. */
  Class<? extends SerdeProtocol>[] value();
}
