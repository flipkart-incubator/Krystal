package com.flipkart.krystal.krystex.commands;

import static com.flipkart.krystal.except.KrystalCompletionException.wrapAsCompletionException;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.DirectResponse;
import java.util.List;

public record DirectForwardSend(
    VajramID vajramID,
    List<? extends RequestResponseFuture<? extends Request<?>, ?>> executableRequests,
    DependentChain dependentChain)
    implements MultiRequestDirectCommand, ClientSideCommand<DirectResponse> {

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }

  @Override
  public DirectForwardSend rerouteTo(VajramID targetVajramID) {
    return new DirectForwardSend(targetVajramID, executableRequests(), dependentChain());
  }

  @Override
  public void error(Throwable throwable) {
    for (RequestResponseFuture<? extends Request<?>, ?> executableRequest : executableRequests()) {
      executableRequest.response().completeExceptionally(wrapAsCompletionException(throwable));
    }
  }
}
