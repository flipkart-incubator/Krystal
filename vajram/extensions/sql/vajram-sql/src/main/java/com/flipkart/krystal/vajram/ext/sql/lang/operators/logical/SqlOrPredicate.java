package com.flipkart.krystal.vajram.ext.sql.lang.operators.logical;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.vajram.ext.sql.lang.SqlWherePredicate;

/**
 * Marker interface for {@link Model}s which represent an OR operator that operates on multiple
 * other {@link SqlWherePredicate}s
 */
public non-sealed interface SqlOrPredicate extends SqlWherePredicate {}
