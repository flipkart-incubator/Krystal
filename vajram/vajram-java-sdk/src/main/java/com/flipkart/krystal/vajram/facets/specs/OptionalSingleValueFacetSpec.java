package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.data.Request;

public sealed interface OptionalSingleValueFacetSpec<T, CV extends Request>
    extends OptionalFacetSpec<T, CV> permits OptionalFacetDefaultSpec, OptionalOne2OneDepSpec {}
