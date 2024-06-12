package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.kryon.KryonId;

record CacheKey(KryonId kryonId, Facets facets) {}
