package com.flipkart.krystal.vajram.guice.executorscope;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.ScopeAnnotation;
import java.lang.annotation.Retention;

/**
 * Custom guice scope which scopes a binding to a given {@link
 * com.flipkart.krystal.krystex.kryon.KryonExecutor}
 */
@Retention(RUNTIME)
@ScopeAnnotation
public @interface KrystalExecutorScoped {}
