package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.WorkflowPayload;
import java.util.List;

public interface TestWorkflowContext2 extends WorkflowPayload {
  void setX1String(String string);

  String getX1String();

  TransformedProduct getInitialTransformedProduct();

  void setInitialTransformedProduct(TransformedProduct transformedProduct);

  // Default header name is inferred from field name
  // By converting camel case to allcaps snake case (TRIGGER_USER_ID)
  String getTriggerUserId();

  void setMetrics(List<Metric> collect);

  boolean isEnableValidation();

  ProductUpdateEventsContainer getProductUpdateEvents();

  void setProductUpdateEvents(ProductUpdateEventsContainer productUpdateEvents);

  void setConditionalTransformedProducts(List<TransformedProduct> transformedProducts);

  List<TransformedProduct> getConditionalTransformedProducts();
}
