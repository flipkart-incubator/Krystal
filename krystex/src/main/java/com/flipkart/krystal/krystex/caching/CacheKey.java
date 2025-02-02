package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.data.ImmutableFacetContainer;
import com.flipkart.krystal.krystex.kryon.KryonId;

record CacheKey(KryonId kryonId, ImmutableFacetContainer facets) {}
