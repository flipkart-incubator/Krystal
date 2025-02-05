package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.data.ImmutableFacetValuesContainer;
import com.flipkart.krystal.krystex.kryon.KryonId;

record CacheKey(KryonId kryonId, ImmutableFacetValuesContainer facets) {}
