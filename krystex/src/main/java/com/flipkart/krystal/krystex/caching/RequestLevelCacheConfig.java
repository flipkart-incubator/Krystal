package com.flipkart.krystal.krystex.caching;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.data.Request;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(TYPE)
@Retention(RUNTIME)
public @interface RequestLevelCacheConfig {
  Class<? extends Request<?>>[] canInvalidateCacheOf() default {};
}
