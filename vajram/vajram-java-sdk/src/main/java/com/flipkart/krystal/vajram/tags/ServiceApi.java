package com.flipkart.krystal.vajram.tags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceApi {

  Service service();

  String apiName();

  String version() default "1";
}
