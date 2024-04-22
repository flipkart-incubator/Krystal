package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.google.common.collect.ImmutableList;

public sealed interface InputResolver extends InputResolverDefinition
    permits AbstractInputResolver {
  DependencyCommand<? extends Request<Object>> resolve(
      ImmutableList<? extends RequestBuilder<Object>> depRequests, Facets facets);

  void setResolverId(int resolverId);
}
