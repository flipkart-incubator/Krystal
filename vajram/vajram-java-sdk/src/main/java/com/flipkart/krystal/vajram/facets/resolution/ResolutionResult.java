package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import java.util.List;
import java.util.Map;

public record ResolutionResult(
    Map</*DepFacetId*/ Integer, List<RequestBuilder>> results,
    Map<Integer, DependencyCommand<? extends Request>> skippedDependencies) {}
