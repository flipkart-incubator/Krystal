package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.krystex.kryon.KryonId;

record CacheKey(KryonId kryonId, ImmutableRequest<Object> facets) {}
