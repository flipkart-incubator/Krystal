package com.flipkart.krystal.krystex.commands;

import static com.flipkart.krystal.except.KrystalCompletionException.wrapAsCompletionException;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonUtils;
import com.flipkart.krystal.krystex.kryon.VajramKryonDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@ToString
public final class DirectForwardCommand implements DirectForwardSend, DirectForwardReceive {
  private final VajramID vajramID;
  private final List<? extends RequestResponseFuture<? extends Request<?>, ?>> executableRequests;
  private final DependentChain dependentChain;

  private @MonotonicNonNull List<ExecutionItem> executionItems;

  public DirectForwardCommand(
      VajramID vajramID,
      List<? extends RequestResponseFuture<? extends Request<?>, ?>> executableRequests,
      DependentChain dependentChain) {
    this.vajramID = vajramID;
    this.executableRequests = executableRequests;
    this.dependentChain = dependentChain;
  }

  public static DirectForwardSend ofExecutionItems(
      VajramID vajramID, List<ExecutionItem> executionItems, DependentChain dependentChain) {
    DirectForwardCommand command =
        new DirectForwardCommand(
            vajramID,
            Lists.transform(
                executionItems,
                executionItem -> {
                  @SuppressWarnings("unchecked")
                  Request<Object> request =
                      (Request<Object>) requireNonNull(executionItem).facetValues()._request();
                  return new RequestResponseFuture<>(request, executionItem.response());
                }),
            dependentChain);
    command.executionItems = executionItems;
    return command;
  }

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }

  @Override
  public DirectForwardCommand rerouteTo(VajramID targetVajramID) {
    return new DirectForwardCommand(targetVajramID, executableRequests(), dependentChain());
  }

  @Override
  public void error(Throwable throwable) {
    for (RequestResponseFuture<? extends Request<?>, ?> executableRequest : executableRequests()) {
      executableRequest.response().completeExceptionally(wrapAsCompletionException(throwable));
    }
  }

  public List<ExecutionItem> executionItems(KryonDefinitionRegistry kryonDefinitionRegistry) {
    if (executionItems == null) {
      VajramKryonDefinition vajramKryonDefinition =
          KryonUtils.validateAsVajram(kryonDefinitionRegistry.getOrThrow(vajramID()));
      executionItems =
          ImmutableList.copyOf(
              Lists.transform(
                  executableRequests,
                  executableRequest -> {
                    @SuppressWarnings("unchecked")
                    CompletableFuture<@Nullable Object> response =
                        (CompletableFuture<@Nullable Object>)
                            requireNonNull(executableRequest).response();
                    return new ExecutionItem(
                        vajramKryonDefinition
                            .facetsFromRequest()
                            .logic()
                            .facetsFromRequest(executableRequest.request()),
                        response);
                  }));
    }
    return executionItems;
  }

  @Override
  public VajramID vajramID() {
    return vajramID;
  }

  public List<? extends RequestResponseFuture<? extends Request<?>, ?>> executableRequests() {
    return executableRequests;
  }

  @Override
  public DependentChain dependentChain() {
    return dependentChain;
  }
}
