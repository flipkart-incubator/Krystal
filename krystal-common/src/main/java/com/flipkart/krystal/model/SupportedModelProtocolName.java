package com.flipkart.krystal.model;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.model.SupportedModelProtocolName.SupportedModelProtocolNames;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

/**
 * This annotation is used to specify the name of the model protocol supported by a type. This is
 * preferred over {@link SupportedModelProtocol} in cases where we want to avoid users of the jar
 * produced by this project from unnecessarily having to add protocol libraries to their classpath
 * which they don't need.
 */
@Repeatable(SupportedModelProtocolNames.class)
@Target(TYPE)
public @interface SupportedModelProtocolName {

  /** THe fully qualified "canonical" class name of the protocol class. */
  String value();

  @Target(TYPE)
  @interface SupportedModelProtocolNames {
    SupportedModelProtocolName[] value();
  }
}
