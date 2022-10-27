package com.flipkart.krystal.caramel.samples.basic;

import static com.flipkart.krystal.caramel.model.WorkflowMeta.workflow;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.conditionalTransformedProducts;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.initProductEvent;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.initialTransformedProduct;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.isEnableValidation;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.metrics;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.nextProduct;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.productUpdateEvents;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.secondString;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.string;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.triggerUserId;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.x1String;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;

import com.flipkart.krystal.caramel.model.Bridge;
import com.flipkart.krystal.caramel.model.InMemSyncChannelBuilder;
import com.flipkart.krystal.caramel.model.InputChannel;
import com.flipkart.krystal.caramel.model.WorkflowCompletionStage;
import com.flipkart.krystal.caramel.samples.basic.SplitPayload.SplitPayloadFields;
import com.flipkart.krystal.caramel.samples.basic.StringMetricPayload.StringMetricFields;
import com.flipkart.krystal.caramel.samples.basic.SubMetricPayload.SubMetricFields;
import com.flipkart.krystal.caramel.samples.basic.TransformedProductPayload.TransformedProductPayloadFields;
import com.flipkart.krystal.caramel.samples.basic.TransformedProductWfPayload.TransformedProductWfFields;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;

/**
 * This class does not do anything meaningful. This is just written as an extensive showcase of all
 * the features of the caramel DSL.
 */
@AllArgsConstructor
public class TestWorkflowBuilder {

  private static final Logger log = Logger.getAnonymousLogger();
  private final Bridge<TransformedProduct> productEventNotificationBridge;
  private final Bridge<TransformedProduct> productEventNotificationBridge2;
  private final Bridge<TransformedProduct> productEventNotificationBridge3;
  private final Bridge<TestPayload> rootContextQueue;
  private final Bridge<TestWorkflowContext2> nextRootContextQueue;
  private final InMemSyncChannelBuilder inMemSyncChannel;
  private final InputChannel<ProductUpdateEventsContainer> variantsChannel;
  private final Bridge<Metric> metricsChannel;
  private final ExecutorService customExecutorService;

  public WorkflowCompletionStage.TerminatedWorkflow<
          ProductUpdateEventsContainer, TestPayload, TransformedProduct>
      createWorkflow() {
    return workflow("ProductClassificationWorkflow", TestPayload.class)
        .take(productUpdateEvents, variantsChannel)
        .compute(x1String, String::valueOf, productUpdateEvents)
        .peek(productUpdateEvents, payload -> log.info(payload.toString()))
        .compute(initialTransformedProduct, this::newTransformedProduct, x1String)
        .peek(() -> log.info("created Test Workflow Context"))
        .peek( // This behaves as a fire and forget since we are peeking with a workflow
            initialTransformedProduct,
            workflow("ProductClassificationSubWorkflow", TransformedProductPayload.class)
                .take(
                    TransformedProductPayloadFields.initialTransformedProduct,
                    productEventNotificationBridge)
                .compute(
                    TransformedProductPayloadFields.x2String,
                    String::valueOf,
                    TransformedProductPayloadFields.initialTransformedProduct)
                .peek(
                    TransformedProductPayloadFields.initialTransformedProduct,
                    TransformedProductPayloadFields.x2String,
                    (itp, x2) -> {
                      log.info(String.valueOf(itp));
                      log.info(x2);
                    })
                .terminateWithOutput(TransformedProductPayloadFields.finalTransformedProduct))
        .fork()
        .withInput(initialTransformedProduct)
        .usingWorkflow(
            workflow("ProductClassificationSubWorkflow2", TransformedProductPayload.class)
                .take(
                    TransformedProductPayloadFields.initialTransformedProduct,
                    productEventNotificationBridge)
                .compute(
                    TransformedProductPayloadFields.x2String,
                    Object::toString,
                    TransformedProductPayloadFields.initialTransformedProduct)
                .peek(
                    TransformedProductPayloadFields.initialTransformedProduct,
                    TransformedProductPayloadFields.x2String,
                    (initialTransformedProduct, x2String) ->
                        log.info(initialTransformedProduct + x2String))
                .compute(
                    TransformedProductPayloadFields.finalTransformedProduct,
                    identity(),
                    TransformedProductPayloadFields.initialTransformedProduct)
                .terminateWithOutput(TransformedProductPayloadFields.x2String))
        .toCompute(triggerUserId, identity())
        .splitAs(
            wf -> toProductEvents(wf.initialTransformedProduct(), wf.triggerUserId()),
            List.of(initialTransformedProduct, triggerUserId))
        .stopOnException()
        .sequentially()
        .processEachWith(
            workflow("splitWorkflow", SplitPayload.class)
                .take(
                    SplitPayloadFields.initProductEvent,
                    inMemSyncChannel.of(ProductUpdateEvent.class))
                .compute(
                    SplitPayloadFields.initString,
                    String::valueOf,
                    SplitPayloadFields.initProductEvent)
                .peek(SplitPayloadFields.initString, log::info)
                .compute(
                    SplitPayloadFields.metric, s -> new Metric(s, 2), SplitPayloadFields.initString)
                .splitAs(Stream::of, SplitPayloadFields.metric)
                .stopOnException()
                .sequentially()
                .processEachWith(
                    workflow("subSplitWorkflow", SubMetricPayload.class)
                        .take(SubMetricFields.init, inMemSyncChannel.of(Metric.class))
                        .compute(SubMetricFields.metric, identity(), SubMetricFields.init)
                        .peek(() -> log.info("subMetricWorkflowContext.getMetric().toString()"))
                        .terminateWithOutput(SubMetricFields.metric))
                .extractEachWith(identity())
                .mergeInto(SplitPayloadFields.metrics, identity())
                .terminateWithOutput(SplitPayloadFields.metrics))
        .extractEachWith(identity())
        .mergeInto(
            metrics,
            metrics -> metrics.stream().flatMap(Collection::stream).collect(Collectors.toList()))
        .splitAs(
            transformedProduct -> transformedProduct.metrics().stream().map(Objects::toString),
            List.of(metrics))
        .stopOnException()
        .processEachWith(
            workflow("subSplitWorkflow", StringMetricPayload.class)
                .take(StringMetricFields.initString, inMemSyncChannel.of(String.class))
                .compute(
                    StringMetricFields.metric, o -> new Metric(o, 1), StringMetricFields.initString)
                .peek(StringMetricFields.metric, metricsChannel)
                .terminateWithOutput(StringMetricFields.metric))
        .merge()
        .peek(
            isEnableValidation,
            initialTransformedProduct,
            (isEnableValidation, initialTransformedProduct) ->
                log.info(
                    "created Test Workflow Context with validation: "
                        + isEnableValidation
                        + " "
                        + initialTransformedProduct))
        .splitAs(
            productUpdateEvents -> productUpdateEvents.getProductUpdateEvents().stream(),
            productUpdateEvents)
        .stopOnException()
        .processEachWith(
            workflow("TransformedProductWf", TransformedProductWfPayload.class)
                .take(
                    TransformedProductWfFields.productUpdateEvent,
                    inMemSyncChannel.of(ProductUpdateEvent.class))
                .compute(
                    TransformedProductWfFields.transformedProduct,
                    productUpdateEvents -> new TransformedProduct(),
                    TransformedProductWfFields.productUpdateEvent)
                .terminateWithOutput(TransformedProductWfFields.transformedProduct))
        .extractEachWith(identity())
        .mergeInto(conditionalTransformedProducts, identity())
        .ifTrue(
            isEnableValidation,
            trueCase -> trueCase.compute(conditionalTransformedProducts, ImmutableList::of))
        .orElse(falseCase -> falseCase.compute(conditionalTransformedProducts, ImmutableList::of))
        .endIf()
        .ifTrue(
            triggerUserId,
            String::isEmpty,
            emptyTriggerUserId ->
                emptyTriggerUserId
                    .ifTrue(
                        isEnableValidation,
                        emptyTriggerUserIdValidationEnabled ->
                            emptyTriggerUserIdValidationEnabled.compute(
                                conditionalTransformedProducts, ArrayList::new))
                    .orElse(
                        emptyTriggerUserIdValidationDisabled ->
                            emptyTriggerUserIdValidationDisabled.compute(
                                conditionalTransformedProducts, ArrayList::new))
                    .endIf())
        .elseIfTrue(
            triggerUserId,
            triggerUserId -> triggerUserId.length() > 10,
            triggerUserIdLongerThanTen ->
                triggerUserIdLongerThanTen.compute(conditionalTransformedProducts, ArrayList::new))
        .orElse(
            triggerUserId0To10 ->
                triggerUserId0To10
                    .ifTrue(
                        isEnableValidation,
                        triggerUserId0To10ValidationEnabled ->
                            triggerUserId0To10ValidationEnabled.compute(
                                conditionalTransformedProducts, ArrayList::new))
                    .orElse(
                        triggerUserId0To10ValidationDisabled ->
                            triggerUserId0To10ValidationDisabled.compute(
                                conditionalTransformedProducts, ArrayList::new))
                    .endIf())
        .endIf()
        .compute(
            initialTransformedProduct,
            conditionalTransformedProducts -> conditionalTransformedProducts.get(0),
            conditionalTransformedProducts)
        .conditional(
            triggerUserId,
            String::isEmpty,
            ifTrue1 ->
                ifTrue1.conditional(
                    isEnableValidation,
                    identity(),
                    ifFalse10 ->
                        ifFalse10.compute(TestPayload.TestPayloadFields.x1String, () -> "hello")),
            ifFalse0 ->
                ifFalse0.conditional(
                    isEnableValidation,
                    ifTrue01 ->
                        ifTrue01.compute(TestPayload.TestPayloadFields.x1String, () -> "hi"),
                    ifFalse00 ->
                        ifFalse00.compute(
                            initialTransformedProduct,
                            payload -> new TransformedProduct(),
                            emptyList())))
        .checkpoint("checkpoint1")
        .compute(string, String::valueOf, initProductEvent)
        .peek(string, log::info)
        .checkpoint("checkpoint2")
        .compute(secondString, () -> "")
        .compute(nextProduct, TransformedProduct::new)
        .terminateWithOutput(nextProduct);
  }

  private TransformedProduct newTransformedProduct(String x1String) {
    return null;
  }

  private Stream<ProductUpdateEvent> toProductEvents(
      TransformedProduct transformedProduct, String triggerUserId) {
    return Stream.of();
  }
}
