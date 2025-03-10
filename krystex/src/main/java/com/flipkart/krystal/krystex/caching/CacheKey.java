package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ImmutableFacetValuesContainer;

record CacheKey(VajramID vajramID, ImmutableFacetValuesContainer facets) {}
