package com.flipkart.krystal.serial;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Facet;
import java.lang.annotation.Target;

/**
 * Provides a way to specify a serialization index od a facet or of a field in a model for binary
 * serialization protocols like protobuf, capnproto, and thrift
 */
@ApplicableToElements(Facet.class)
@Target({FIELD, METHOD})
public @interface SerialId {
  int value();
}
