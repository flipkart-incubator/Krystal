package com.flipkart.krystal.core;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.facets.Dependency;
import java.util.List;

public interface CommunicationFacade {
  void triggerDependency(
      Dependency dependency,
      List<? extends RequestResponseFuture<? extends Request<?>, ?>> responseFutures);

  void executeOutputLogic();
}
