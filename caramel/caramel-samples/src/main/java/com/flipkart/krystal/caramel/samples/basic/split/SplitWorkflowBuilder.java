package com.flipkart.krystal.caramel.samples.basic.split;

import static com.flipkart.krystal.caramel.model.WorkflowMeta.workflow;
import static com.flipkart.krystal.caramel.samples.basic.split.SplitPayload.SplitPayloadFields.initProductEvent;
import static com.flipkart.krystal.caramel.samples.basic.split.SplitPayload.SplitPayloadFields.initString;
import static com.flipkart.krystal.caramel.samples.basic.split.SplitPayload.SplitPayloadFields.metric;
import static com.flipkart.krystal.caramel.samples.basic.split.SplitPayload.SplitPayloadFields.metrics;
import static com.flipkart.krystal.caramel.samples.basic.stringconversion.StringConversionWorkflowBuilder.convertToString;
import static java.util.function.Function.identity;

import com.flipkart.krystal.caramel.model.TerminatedWorkflow;
import com.flipkart.krystal.caramel.samples.basic.Metric;
import com.flipkart.krystal.caramel.samples.basic.ProductUpdateEvent;
import com.flipkart.krystal.caramel.samples.basic.split.SubMetricPayload.SubMetricFields;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SplitWorkflowBuilder {

  private static final Logger log = Logger.getLogger("");

  public static TerminatedWorkflow<ProductUpdateEvent, SplitPayload, Collection<Metric>>
      splitter() {
    return workflow("splitWorkflow", SplitPayload.class)
        .startWith(initProductEvent)
        .compute(initString, convertToString(), initProductEvent)
        .peek(initString, log::info)
        .compute(metric, s -> new Metric(s, 2), initString)
        .splitAs(Stream::of, metric)
        .stopOnException()
        .sequentially()
        .processEachWith(
            workflow("subSplitWorkflow", SubMetricPayload.class)
                .startWith(SubMetricFields.init)
                .compute(SubMetricFields.metric, identity(), SubMetricFields.init)
                .peek(() -> log.info("subMetricWorkflowContext.getMetric().toString()"))
                .terminateWithOutput(SubMetricFields.metric))
        .toCompute(metrics)
        .terminateWithOutput(metrics);
  }
}
