package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.CaramelField;
import com.flipkart.krystal.caramel.model.SimpleCaramelField;
import com.flipkart.krystal.caramel.model.Value;
import com.flipkart.krystal.caramel.model.ValueImpl;

public class StringMetricPayload implements StringMetricPayloadDefinition {
  interface StringMetricFields {
    CaramelField<String, StringMetricPayload> initString =
        new SimpleCaramelField<>(
            "initString",
            StringMetricPayload.class,
            StringMetricPayload::initString,
            StringMetricPayload::setInitString);

    CaramelField<Metric, StringMetricPayload> metric =
        new SimpleCaramelField<>(
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
