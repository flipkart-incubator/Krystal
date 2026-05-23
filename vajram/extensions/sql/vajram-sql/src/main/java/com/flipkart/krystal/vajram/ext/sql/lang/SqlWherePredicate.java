package com.flipkart.krystal.vajram.ext.sql.lang;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.logical.SqlOrPredicate;

public sealed interface SqlWherePredicate extends Model
    permits SqlOrPredicate, SelectionPredicate {}
