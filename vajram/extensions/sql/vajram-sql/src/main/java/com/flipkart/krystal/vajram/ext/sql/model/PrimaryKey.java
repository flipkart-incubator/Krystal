package com.flipkart.krystal.vajram.ext.sql.model;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/** Annotation to mark a field in a @{@link Table} model as a primary key. */
@Target(METHOD)
public @interface PrimaryKey {}
