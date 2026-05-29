package com.flipkart.krystal.lattice.ext.rest;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.vajram.VajramDef;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Inherited
@Target({TYPE, METHOD})
@DopantType(RestServiceDopant.REST_SERVICE_DOPANT_TYPE)
public @interface RestService {

  /**
   * The vajrams that define the resources of the rest service. The REST Verb (GET, POST, PUT,
   * DELETE) is specified by the vajram.
   */
  Class<? extends VajramDef<?>>[] resourceVajrams();

  /**
   * The common prefix for all the resources defined by this rest service. For example, if the path
   * prefix is "api", then the Vajram "getUser" will be accessible at "api/getUser".
   */
  String pathPrefix() default "";
}
