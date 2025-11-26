package com.flipkart.krystal.lattice.ext.rest;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.vajram.VajramDef;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Inherited
@Target(TYPE)
@DopantType(RestServiceDopant.REST_SERVICE_DOPANT_TYPE)
public @interface RestService {

  Class<? extends VajramDef<?>>[] resourceVajrams();

  String pathPrefix() default "";
}
