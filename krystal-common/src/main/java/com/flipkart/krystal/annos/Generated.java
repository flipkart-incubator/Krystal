package com.flipkart.krystal.annos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark source code that has been generated.
 *
 * <p>This can be useful, for example, to developers to infer that changes made to classes with this
 * annotation may be overwritten, and to code coverage tools as they can exclude generated sources
 * from code coverage reports.
 *
 * @implNote The @Generated annotation in 'javax.annotation.processing' package was created for this
 *     purpose. But tools like jacoco need at least CLASS retention to read the annotation. But the
 *     'javax.annotation.processing.Generated' annotation only has SOURCE retention. This is the
 *     reason this annotation had to be created.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Generated {

  /**
   * The code generator which generated this code.
   *
   * @return The fully qualified name of the code generator.
   */
  String by() default "";
}
