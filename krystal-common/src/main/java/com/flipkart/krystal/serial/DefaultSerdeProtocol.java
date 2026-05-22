package com.flipkart.krystal.serial;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import com.flipkart.krystal.model.ModelRoot;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * For a given @{@link ModelRoot}, specifies the default serde protocol to be used when clients have
 * not specified any protocol explicitly (using Http Accept header, for example). If a ModelRoot
 * does not specify a DefaultSerdeProtocol, then handling defaulting behaviour is left the
 * application logic which can chose to default or even throw an error.
 */
@Target(TYPE)
@Retention(CLASS)
public @interface DefaultSerdeProtocol {
  Class<? extends SerdeProtocol> value();
}
