package com.flipkart.krystal.vajram.ext.sql.lang;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.model.Model;
import java.lang.annotation.Target;

/** Annotation to mark a @{@link Model} interface as an SQL INSERT statement. */
@Target(TYPE)
public @interface INSERT {}
