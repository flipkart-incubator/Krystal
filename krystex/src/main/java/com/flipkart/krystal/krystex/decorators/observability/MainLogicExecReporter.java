package com.flipkart.krystal.krystex.decorators.observability;

import static com.flipkart.krystal.data.ValueOrError.empty;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.allOf;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

public class MainLogicExecReporter implements MainLogicDecorator {

  private final NodeExecutionReport nodeExecutionReport;

  public MainLogicExecReporter(NodeExecutionReport nodeExecutionReport) {
    this.nodeExecutionReport = nodeExecutionReport;
  }

  @Override
  public MainLogic<Object> decorateLogic(
      MainLogic<Object> logicToDecorate, MainLogicDefinition<Object> originalLogicDefinition) {
    return inputs -> {
      NodeId nodeId = originalLogicDefinition.nodeLogicId().nodeId();
      NodeLogicId nodeLogicId = originalLogicDefinition.nodeLogicId();
      /*
       Report logic start
      */
      nodeExecutionReport.reportMainLogicStart(nodeId, nodeLogicId, inputs);

      /*
       Execute logic
      */
      ImmutableMap<Inputs, CompletableFuture<Object>> results = logicToDecorate.execute(inputs);
      /*
       Report logic start
      */
      allOf(results.values().toArray(CompletableFuture[]::new))
          .whenComplete(
              (unused, throwable) -> {
                nodeExecutionReport.reportMainLogicEnd(
                    nodeId,
                    nodeLogicId,
                    new Results<>(
                        results.entrySet().stream()
                            .collect(
                                toImmutableMap(
                                    Entry::getKey,
                                    e ->
                                        e.getValue()
                                            .handle(ValueOrError::valueOrError)
                                            .getNow(empty())))));
              });
      return results;
    };
  }

  @Override
  public String getId() {
    return MainLogicExecReporter.class.getName();
  }
}
