package com.flipkart.krystal.vajram.ext.sql.lang;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.vajram.ext.sql.model.Table;

/**
 * Marker interface for {@link Model}s which represent SQL WHERE clauses which filter based on
 * column values of a {@link Table}. When an ColumnPredicate declares multiple columns in the
 * predicate, then these are combined with an AND operator.
 */
public non-sealed interface ColumnPredicate extends SqlWherePredicate {}
