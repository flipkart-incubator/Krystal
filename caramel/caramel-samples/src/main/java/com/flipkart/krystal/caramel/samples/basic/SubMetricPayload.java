package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.AbstractValue;
import com.flipkart.krystal.caramel.model.Field;
import com.flipkart.krystal.caramel.model.SimpleField;

public class SubMetricPayload implements SubMetricPayloadDefinition {

  public interface SubMetricFields {
    Field<Metric, SubMetricPayload> init =
        new SimpleField<>(
            "init", SubMetricPayload.class, SubMetricPayload::init, SubMetricPayload::setInit);
    Field<Metric, SubMetricPayload> metric =
        new SimpleField<>(
            "metric", SubMetricPayload.class, SubMetricPayload::init, SubMetricPayload::setInit);
  }

  private final Value<Metric> init = new Value<>(SubMetricFields.init);
  private final Value<Metric> metric = new Value<>(SubMetricFields.metric);

  @Override
  public Metric init() {
    return init.getOrThrow();
  }

  public void setInit(Metric init) {
    this.init.set(init);
  }

  @Override
  public Metric getMetric() {
    return metric.getOrThrow();
  }

  public void setMetric(Metric metric) {
    this.metric.set(metric);
  }

  private final class Value<T> extends AbstractValue<T, SubMetricPayload> {

    private final Field<T, SubMetricPayload> field;

    public Value(Field<T, SubMetricPayload> field) {
      this.field = field;
    }

    @Override
    public Field<T, SubMetricPayload> field() {
      return field;
    }

    @Override
    public SubMetricPayload getPayload() {
      return SubMetricPayload.this;
    }
  }
}
