package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;

public record Resolver(KryonLogicId resolverKryonLogicId, ResolverDefinition definition) {}
