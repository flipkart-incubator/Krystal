package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation on a Vajram class which tells the vajram SDK that this vajram needs to be processed
 * by the Vajram annotation processors to generate model java classes and impl java classes by
 * looking for the relevant annotations on the fields and methods in the vajram. Examples of these
 * annotations are {@link Input}, {@link Dependency}, @{@link Resolve}, @{@link Output}, etc
 *
 * <p>In a vajram class hierarchy where classA extends classB etc., the simpleClassName of the class
 * which has this annotation will be used as the vajram id .
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VajramDef {

  /**
   * Default is empty - which means infer from the class name.
   *
   * <p>Currently custom vajramIds are not supported. If this value digresses from the VajramClass
   * name, then an exception is thrown while loading the vajram. Developers must skip this field and
   * let the SDK infer the vajramId from the class name.
   *
   * @return the id of this vajram
   */
  String vajramId() default "";

  /**
   * Default is {@link ComputeDelegationType#DEFAULT} which means this is inferred from the class
   * hierarchy. A vajram which extends {@link ComputeVajram} gets the value {@link
   * ComputeDelegationType#NO_DELEGATION} and a class which extends {@link IOVajram} gets the value
   * {@link ComputeDelegationType#SYNC_DELEGATION}
   *
   * <p>Vajram developers are expected to leave this field empty and let the SDK infer the value
   * from the code. Else an error is thrown
   *
   * @return the type of delegation that this vajram's output logic uses
   */
  ComputeDelegationType computeDelegationType() default ComputeDelegationType.DEFAULT;
}
