package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.AbstractValue;
import com.flipkart.krystal.caramel.model.Field;
import com.flipkart.krystal.caramel.model.SimpleField;

public class StringMetricPayload implements StringMetricPayloadDefinition {
  interface StringMetricFields {
    Field<String, StringMetricPayload> initString =
        new SimpleField<>(
            "initString",
            StringMetricPayload.class,
            StringMetricPayload::initString,
            StringMetricPayload::setInitString);

    Field<Metric, StringMetricPayload> metric =
        new SimpleField<>(
            "metric",
            StringMetricPayload.class,
            StringMetricPayload::metric,
            StringMetricPayload::setMetric);
  }

  private final Value<String> initString = new Value<>(StringMetricFields.initString);
  private final Value<Metric> metric = new Value<>(StringMetricFields.metric);

  @Override
  public String initString() {
    return initString.getOrThrow();
  }

  public void setInitString(String s) {
    this.initString.set(s);
  }

  @Override
  public Metric metric() {
    return metric.getOrThrow();
  }

  public void setMetric(Metric metric) {
    this.metric.set(metric);
  }

  private final class Value<T> extends AbstractValue<T, StringMetricPayload> {

    private final Field<T, StringMetricPayload> field;

    public Value(Field<T, StringMetricPayload> field) {
      this.field = field;
    }

    @Override
    public Field<T, StringMetricPayload> field() {
      return field;
    }

    @Override
    public StringMetricPayload getPayload() {
      return StringMetricPayload.this;
    }
  }
}
