package com.flipkart.krystal.vajram.ext.sql.model;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/**
 * Annotation to mark a field in a @{@link Table} model as a foreign key to another Table. The field
 * type must be same as the model type of the otherTable.
 */
@Target(METHOD)
public @interface ForeignKey {}
