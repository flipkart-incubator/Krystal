package com.flipkart.krystal.core;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import java.util.List;

public interface CommunicationFacade {
  <T, R extends Request<T>> void triggerDependency(
      List<RequestResponseFuture<R, T>> responseFutures);

  <T> void executeOutputLogic(FacetValues facetValues);
}
