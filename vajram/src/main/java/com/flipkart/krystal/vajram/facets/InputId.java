package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.vajram.das.DataAccessSpec;

public record InputId<T>(DataAccessSpec accessSpec, String inputName) {}
