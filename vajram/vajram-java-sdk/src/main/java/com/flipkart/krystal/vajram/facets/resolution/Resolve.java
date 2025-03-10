package com.flipkart.krystal.vajram.facets.resolution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Resolve {

  /**
   * The qualified facet name of the dependency facet which is being resolved. A qualified facet
   * name looks like {@code "CurrentVajramId:dependencyFacetName"}. Developers should not pass a
   * hand-written string here. The Vajram SDK code generator generates the qualified names for all
   * facets in the vajram facets interface ({@code VajramId_Fac}) as static constants named as
   * {@code facetName_n}. These auto-generated string constants should be used for this parameter to
   * avoid manual errors.
   */
  String dep();

  /**
   * One or more qualified facet names of the inputs of the dependency that are being resolved. A
   * qualified facet name looks like {@code "DependencyVajramId:inputName"}.Developers should not
   * pass hand-written strings here. The Vajram SDK code generator generates the qualified names for
   * all inputs in the vajram request interface ({@code VajramId_Req}. These auto-generated string
   * constants should be used for this parameter to avoid manual errors.
   */
  String[] depInputs();
}
