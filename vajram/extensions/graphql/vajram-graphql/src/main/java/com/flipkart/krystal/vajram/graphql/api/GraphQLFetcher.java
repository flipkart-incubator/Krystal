package com.flipkart.krystal.vajram.graphql.api;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a given vajram is responsible for directly providing data to one or more fields in
 * a graphQL schema
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@ApplicableToElements(KrystalElement.Vajram.class)
public @interface GraphQLFetcher {}
