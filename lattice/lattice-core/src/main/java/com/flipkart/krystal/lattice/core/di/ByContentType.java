package com.flipkart.krystal.lattice.core.di;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Injection;
import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;

@Qualifier
@ApplicableToElements(Injection.class)
@Retention(RUNTIME)
public @interface ByContentType {}
