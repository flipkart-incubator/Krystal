package com.flipkart.krystal.caramel.samples.basic.split;

import com.flipkart.krystal.caramel.model.Field;
import com.flipkart.krystal.caramel.model.SimpleField;
import com.flipkart.krystal.caramel.model.Value;
import com.flipkart.krystal.caramel.model.ValueImpl;
import com.flipkart.krystal.caramel.samples.basic.Metric;

public class SubMetricPayload implements SubMetricPayloadDefinition {

  public interface SubMetricFields {
    Field<Metric, SubMetricPayload> init =
        new SimpleField<>(
            "init", SubMetricPayload.class, SubMetricPayload::init, SubMetricPayload::setInit);
    Field<Metric, SubMetricPayload> metric =
        new SimpleField<>(
            "metric", SubMetricPayload.class, SubMetricPayload::init, SubMetricPayload::setInit);
  }

  private final Value<Metric, SubMetricPayload> init = new ValueImpl<>(SubMetricFields.init, this);
  private final Value<Metric, SubMetricPayload> metric =
      new ValueImpl<>(SubMetricFields.metric, this);

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
}
