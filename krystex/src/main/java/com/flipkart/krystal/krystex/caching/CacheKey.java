package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonResponse;

record CacheKey(Kryon<KryonCommand, KryonResponse> kryon, Facets facets) {}
