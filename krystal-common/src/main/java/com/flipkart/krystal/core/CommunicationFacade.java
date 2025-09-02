package com.flipkart.krystal.core;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.Request;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CommunicationFacade {
  <T> void triggerDependency(Request<T> request, CompletableFuture<T> responseFuture);

  <T> CompletableFuture<T> triggerDependencyFanout(List<Request<T>> request);

  <T> void executeOutputLogic(FacetValues facetValues);
}
