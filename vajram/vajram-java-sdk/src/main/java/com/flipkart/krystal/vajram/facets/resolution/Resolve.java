package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
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
   * One or more qualified facet names of the inputs of the dependency that are being resolved.
   *
   * <p>If this value is empty, it is interpreted as resolving all the inputs of dependency -
   * including any future inputs added to the dependency. If a vajram has a resolver with this value
   * empty, then it MUST NOT have any other resolvers (neither @Resolve methods or in {@code
   * getSimpleInputResolvers()}.
   *
   * <p>When this value is empty (meaning resolving all) or has more than one value, the resolve
   * MUST return a {@code <Dep>_ReqImmut.Builder} (optionally wrapped in a {@link One2OneCommand} or
   * a {@link FanoutCommand}) with the all the inputs set (optional ones can be omitted).
   *
   * <p>A qualified facet name looks like {@code "DependencyVajramId:inputName"}. Developers should
   * not pass handwritten strings here. The Vajram SDK code generator generates the qualified names
   * for all inputs in the vajram request interface ({@code VajramId_Req}. These auto-generated
   * string constants should be used for this parameter to avoid manual errors.
   */
  String[] depInputs() default {};
}
