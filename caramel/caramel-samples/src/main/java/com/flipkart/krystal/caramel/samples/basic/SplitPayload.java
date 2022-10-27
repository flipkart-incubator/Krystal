package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.AbstractValue;
import com.flipkart.krystal.caramel.model.Field;
import com.flipkart.krystal.caramel.model.SimpleField;
import java.util.Collection;

class SplitPayload implements SplitPayloadDefinition {
  interface SplitPayloadFields {
    Field<String, SplitPayload> initString =
        new SimpleField<>(
            "initString",
            SplitPayload.class,
            SplitPayload::initString,
            SplitPayload::setInitString);
    Field<Metric, SplitPayload> metric =
        new SimpleField<>(
            "metric", SplitPayload.class, SplitPayload::metric, SplitPayload::setMetric);
    Field<ProductUpdateEvent, SplitPayload> initProductEvent =
        new SimpleField<>(
            "initProductEvent",
            SplitPayload.class,
            SplitPayload::initProductEvent,
            SplitPayload::setInitProductEvent);
    Field<Collection<Metric>, SplitPayload> metrics =
        new SimpleField<>(
            "metrics", SplitPayload.class, SplitPayload::metrics, SplitPayload::setMetrics);
  }

  private final Value<String> initString = new Value<>(SplitPayloadFields.initString);
  private final Value<Metric> metric = new Value<>(SplitPayloadFields.metric);
  private final Value<ProductUpdateEvent> initProductEvent =
      new Value<>(SplitPayloadFields.initProductEvent);
  private final Value<Collection<Metric>> metrics = new Value<>(SplitPayloadFields.metrics);

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

  private final class Value<T> extends AbstractValue<T, SplitPayload> {

    private final Field<T, SplitPayload> field;

    public Value(Field<T, SplitPayload> field) {
      this.field = field;
    }

    @Override
    public Field<T, SplitPayload> field() {
      return field;
    }

    @Override
    public SplitPayload getPayload() {
      return SplitPayload.this;
    }
  }
}
