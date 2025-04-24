package com.flipkart.krystal.annos;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.core.KrystalElement.Facet;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@ApplicableToElements(Facet.Dependency.class)
@Target(ElementType.FIELD)
@Retention(RUNTIME)
public @interface TraitDependency {}
