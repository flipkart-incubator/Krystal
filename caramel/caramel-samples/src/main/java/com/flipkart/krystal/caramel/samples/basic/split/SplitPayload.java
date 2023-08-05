package com.flipkart.krystal.caramel.samples.basic.split;

import com.flipkart.krystal.caramel.model.CaramelField;
import com.flipkart.krystal.caramel.model.SimpleCaramelField;
import com.flipkart.krystal.caramel.model.Value;
import com.flipkart.krystal.caramel.model.ValueImpl;
import com.flipkart.krystal.caramel.samples.basic.Metric;
import com.flipkart.krystal.caramel.samples.basic.ProductUpdateEvent;
import java.util.Collection;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;

class SplitPayload implements SplitPayloadDefinition {

  SplitPayload() {
    metrics = new ValueImpl<>(SplitPayloadFields.metrics, this);
    initProductEvent = new ValueImpl<>(SplitPayloadFields.initProductEvent, this);
    metric = new ValueImpl<>(SplitPayloadFields.metric, this);
    initString = new ValueImpl<>(SplitPayloadFields.initString, this);
  }

  interface SplitPayloadFields {
    CaramelField<String, SplitPayload> initString =
        new SimpleCaramelField<>(
            "initString",
            SplitPayload.class,
            SplitPayload::initString,
            SplitPayload::setInitString);
    CaramelField<Metric, SplitPayload> metric =
        new SimpleCaramelField<>(
            "metric", SplitPayload.class, SplitPayload::metric, SplitPayload::setMetric);
    CaramelField<ProductUpdateEvent, SplitPayload> initProductEvent =
        new SimpleCaramelField<>(
            "initProductEvent",
            SplitPayload.class,
            SplitPayload::initProductEvent,
            SplitPayload::setInitProductEvent);
    CaramelField<Collection<Metric>, SplitPayload> metrics =
        new SimpleCaramelField<>(
            "metrics", SplitPayload.class, SplitPayload::metrics, SplitPayload::setMetrics);
  }

  @NotOnlyInitialized private final Value<String, SplitPayload> initString;
  @NotOnlyInitialized private final Value<Metric, SplitPayload> metric;
  @NotOnlyInitialized private final Value<ProductUpdateEvent, SplitPayload> initProductEvent;
  @NotOnlyInitialized
  private final Value<Collection<Metric>, SplitPayload> metrics;

  @Override
  public String initString() {
    return initString.getOrThrow();
  }

  public void setInitString(String initString) {
    this.initString.set(initString);
  }

  @Override
  public Metric metric() {
    return metric.getOrThrow();
  }

  public void setMetric(Metric metric) {
    this.metric.set(metric);
  }

  @Override
  public ProductUpdateEvent initProductEvent() {
    return initProductEvent.getOrThrow();
  }

  public void setInitProductEvent(ProductUpdateEvent initProductEvent) {
    this.initProductEvent.set(initProductEvent);
  }

  public void setMetrics(Collection<Metric> metrics) {
    this.metrics.set(metrics);
  }

  @Override
  public Collection<Metric> metrics() {
    return metrics.getOrThrow();
  }
}
