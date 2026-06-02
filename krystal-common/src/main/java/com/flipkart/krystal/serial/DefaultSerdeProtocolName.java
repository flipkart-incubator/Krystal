package com.flipkart.krystal.serial;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Target;

/**
 * Similar to {@link DefaultSerdeProtocol} but preferred in scenarios where we want to avoid adding
 * the protocol classes to client systems' classpath - for example when the type is part of a shared
 * library.
 */
@Target({TYPE, TYPE_USE})
public @interface DefaultSerdeProtocolName {
  String value();
}
