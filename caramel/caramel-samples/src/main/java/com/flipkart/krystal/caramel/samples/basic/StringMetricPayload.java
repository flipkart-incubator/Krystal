package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.Field;
import com.flipkart.krystal.caramel.model.SimpleField;
import com.flipkart.krystal.caramel.model.Value;
import com.flipkart.krystal.caramel.model.ValueImpl;

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

  private final Value<String, StringMetricPayload> initString =
      new ValueImpl<>(StringMetricFields.initString, this);
  private final Value<Metric, StringMetricPayload> metric =
      new ValueImpl<>(StringMetricFields.metric, this);

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
}
