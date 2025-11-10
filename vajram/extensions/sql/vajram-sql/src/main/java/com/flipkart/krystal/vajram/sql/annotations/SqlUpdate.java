package com.flipkart.krystal.vajram.sql.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to mark a trait as a SQL update. The value of the annotation should be the SQL insert/update/delete string.
 * Once the trait is annotated with this annotation, the SQL vajram code generator will generate the necessary code to execute the SQL update.
 * Example usage: @SqlUpdate("UPDATE users SET name = ? WHERE id = ?").
 * Parameters should be provided in the _Inputs class of the trait.
 */
@Retention(CLASS)
@Target(TYPE)
public @interface SqlUpdate {
  String value();
}
