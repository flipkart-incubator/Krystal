package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.data.Request;

public sealed interface MandatorySingleValueFacetSpec<T, CV extends Request>
    extends MandatoryFacetSpec<T, CV> permits MandatoryFacetDefaultSpec, MandatoryOne2OneDepSpec {}
