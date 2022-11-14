package com.flipkart.krystal.caramel.samples.basic;

import static com.flipkart.krystal.caramel.model.OutputChannel.outputChannel;
import static com.flipkart.krystal.caramel.model.WorkflowMeta.workflow;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.conditionalTransformedProducts;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.initProductEvent;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.initialTransformedProduct;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.isEnableValidation;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.metricNames;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.metrics;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.nextProduct;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.productUpdateEvents;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.secondString;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.string;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.triggerUserId;
import static com.flipkart.krystal.caramel.samples.basic.TestPayload.TestPayloadFields.x1String;
import static com.flipkart.krystal.caramel.samples.basic.classification.ProductClassification2WorkflowBuilder.classifyProduct2;
import static com.flipkart.krystal.caramel.samples.basic.classification.ProductClassificationWorkflowBuilder.classifyProduct;
import static com.flipkart.krystal.caramel.samples.basic.split.SplitWorkflowBuilder.splitter;
import static com.flipkart.krystal.caramel.samples.basic.stringconversion.StringConversionWorkflowBuilder.convertToString;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;

import com.flipkart.krystal.caramel.model.TerminatedWorkflow;
import com.flipkart.krystal.caramel.samples.basic.StringMetricPayload.StringMetricFields;
import com.flipkart.krystal.caramel.samples.basic.TransformedProductWfPayload.TransformedProductWfFields;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;

/**
 * This class does not do anything meaningful. This is just written as an extensive showcase of all
 * the features of the caramel DSL.
 */
@AllArgsConstructor
public class ProductProcessingWorkflowBuilder {

  private static final Logger log = Logger.getLogger("");

  private static TerminatedWorkflow<ProductUpdateEventsContainer, TestPayload, TransformedProduct>
      processProduct() {
    return workflow("ProductProcessingWorkflow", TestPayload.class)
        .startWith(productUpdateEvents)
        .compute(x1String, convertToString(), productUpdateEvents)
        .peek(productUpdateEvents, payload -> log.info(payload.toString()))
        .compute(
            initialTransformedProduct,
            ProductProcessingWorkflowBuilder::newTransformedProduct,
            x1String)
        .peek(() -> log.info("created Test Workflow Context"))
        .peek( // This behaves as a fire and forget since we are peeking with a workflow
            initialTransformedProduct, classifyProduct())
        .fork()
        .withInput(initialTransformedProduct)
        .usingWorkflow(classifyProduct2())
        .toCompute(triggerUserId, identity())
        .splitAs(
            wf -> toProductEvents(wf.initialTransformedProduct(), wf.triggerUserId()),
            List.of(initialTransformedProduct, triggerUserId))
        .stopOnException()
        .sequentially()
        .processEachWith(splitter())
        .extractEachWith(l -> l.stream().map(Metric::name).toList())
        .toCompute(
            metricNames,
            metrics -> metrics.stream().flatMap(Collection::stream).collect(Collectors.toList()))
        .compute(metrics, names -> names.stream().map(s -> new Metric(s, 0)).toList(), metricNames)
        .splitAs(
            transformedProduct -> transformedProduct.metrics().stream().map(Objects::toString),
            List.of(metrics))
        .stopOnException()
        .processEachWith(
            workflow("subSplitWorkflow", StringMetricPayload.class)
                .startWith(StringMetricFields.initString)
                .compute(
                    StringMetricFields.metric, o -> new Metric(o, 1), StringMetricFields.initString)
                .peek(StringMetricFields.metric, outputChannel("metrics_channel"))
                .terminateWithOutput(StringMetricFields.metric))
        .toCompute(metrics)
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
                .startWith(TransformedProductWfFields.productUpdateEvent)
                .compute(
                    TransformedProductWfFields.transformedProduct,
                    productUpdateEvents -> new TransformedProduct(),
                    TransformedProductWfFields.productUpdateEvent)
                .terminateWithOutput(TransformedProductWfFields.transformedProduct))
        .toCompute(conditionalTransformedProducts)
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
        .compute(string, convertToString(), initProductEvent)
        .peek(string, log::info)
        .checkpoint("checkpoint2")
        .compute(secondString, () -> "")
        .compute(nextProduct, TransformedProduct::new)
        .terminateWithOutput(nextProduct);
  }

  private static TransformedProduct newTransformedProduct(String x1String) {
    return null;
  }

  private static Stream<ProductUpdateEvent> toProductEvents(
      TransformedProduct transformedProduct, String triggerUserId) {
    return Stream.of();
  }
}
