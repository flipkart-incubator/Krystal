package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.data.ImmutableFacetValuesContainer;
import com.flipkart.krystal.core.VajramID;

record CacheKey(VajramID vajramID, ImmutableFacetValuesContainer facets) {}
