package com.flipkart.krystal.krystex.logicdecorators.observability;

import static com.flipkart.krystal.data.Errable.empty;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.allOf;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MainLogicExecReporter implements OutputLogicDecorator {

  private final KryonExecutionReport kryonExecutionReport;

  public MainLogicExecReporter(KryonExecutionReport kryonExecutionReport) {
    this.kryonExecutionReport = kryonExecutionReport;
  }

  @Override
  public OutputLogic<Object> decorateLogic(
      OutputLogic<Object> logicToDecorate, OutputLogicDefinition<Object> originalLogicDefinition) {
    return inputs -> {
      KryonId kryonId = originalLogicDefinition.kryonLogicId().kryonId();
      KryonLogicId kryonLogicId = originalLogicDefinition.kryonLogicId();
      /*
       Report logic start
      */
      kryonExecutionReport.reportMainLogicStart(kryonId, kryonLogicId, inputs);

      /*
       Execute logic
      */
      ImmutableMap<Facets, CompletableFuture<@Nullable Object>> results =
          logicToDecorate.execute(inputs);
      /*
       Report logic end
      */
      allOf(results.values().toArray(CompletableFuture[]::new))
          .whenComplete(
              (unused, throwable) -> {
                kryonExecutionReport.reportMainLogicEnd(
                    kryonId,
                    kryonLogicId,
                    new Results<>(
                        results.entrySet().stream()
                            .collect(
                                toImmutableMap(
                                    Entry::getKey,
                                    e ->
                                        e.getValue()
                                            .handle(Errable::errableFrom)
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
