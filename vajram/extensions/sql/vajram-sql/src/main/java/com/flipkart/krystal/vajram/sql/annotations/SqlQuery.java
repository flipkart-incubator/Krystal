package com.flipkart.krystal.vajram.sql.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to mark a trait as a SQL query. The value of the annotation should be the SQL query string.
 * Once the trait is annotated with this annotation, the SQL vajram code generator will generate the necessary code to execute the SQL query.
 * Example usage: @SqlQuery("SELECT * FROM users WHERE id = ?").
 * Parameters should be provided in the _Inputs class of the trait.
 */
@Retention(CLASS)
@Target(TYPE)
public @interface SqlQuery {
  String value();
}
