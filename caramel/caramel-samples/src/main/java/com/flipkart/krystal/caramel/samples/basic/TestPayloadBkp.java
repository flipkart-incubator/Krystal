package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.AccessBeforeInitializationException;
import com.flipkart.krystal.caramel.model.CaramelField;
import com.flipkart.krystal.caramel.model.SimpleCaramelField;
import com.flipkart.krystal.caramel.model.Value;
import com.flipkart.krystal.caramel.model.ValueImpl;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;

// AutoGenerated and managed by Caramel
final class TestPayloadBkp implements TestPayloadDefinition {

  TestPayloadBkp() {
    x1String = new ValueImpl<>(TestPayloadFields.x1String, this);
    productUpdateEvents = new ValueImpl<>(TestPayloadFields.productUpdateEvents, this);
    initialTransformedProduct = new ValueImpl<>(TestPayloadFields.initialTransformedProduct, this);
    initProductEvent = new ValueImpl<>(TestPayloadFields.initProductEvent, this);
    conditionalTransformedProducts =
        new ValueImpl<>(TestPayloadFields.conditionalTransformedProducts, this);
    triggerUserId = new ValueImpl<>(TestPayloadFields.triggerUserId, this);
    _metrics = new ValueImpl<>(TestPayloadFields.metrics, this);
    metricNames = new ValueImpl<>(TestPayloadFields.metricNames, this);
    isEnableValidation = new ValueImpl<>(TestPayloadFields.isEnableValidation, this);
    string = new ValueImpl<>(TestPayloadFields.string, this);
    secondString = new ValueImpl<>(TestPayloadFields.secondString, this);
    nextProduct = new ValueImpl<>(TestPayloadFields.nextProduct, this);
  }

  interface TestPayloadFields {

    CaramelField<String, TestPayloadBkp> x1String =
        new SimpleCaramelField<>(
            "x1String",
            TestPayloadBkp.class,
            TestPayloadBkp::x1String,
            TestPayloadBkp::setX1String);
    CaramelField<ProductUpdateEventsContainer, TestPayloadBkp> productUpdateEvents =
        new SimpleCaramelField<>(
            "productUpdateEvents",
            TestPayloadBkp.class,
            TestPayloadBkp::productUpdateEvents,
            TestPayloadBkp::setProductUpdateEvents);
    CaramelField<TransformedProduct, TestPayloadBkp> initialTransformedProduct =
        new SimpleCaramelField<>(
            "initialTransformedProduct",
            TestPayloadBkp.class,
            TestPayloadBkp::initialTransformedProduct,
            TestPayloadBkp::setInitialTransformedProduct);
    CaramelField<ProductUpdateEvent, TestPayloadBkp> initProductEvent =
        new SimpleCaramelField<>(
            "initProductEvent",
            TestPayloadBkp.class,
            TestPayloadBkp::initProductEvent,
            TestPayloadBkp::setInitProductEvent);
    CaramelField<List<TransformedProduct>, TestPayloadBkp> conditionalTransformedProducts =
        new SimpleCaramelField<>(
            "conditionalTransformedProducts",
            TestPayloadBkp.class,
            TestPayloadBkp::conditionalTransformedProducts,
            TestPayloadBkp::setConditionalTransformedProducts);
    CaramelField<String, TestPayloadBkp> triggerUserId =
        new SimpleCaramelField<>(
            "triggerUserId",
            TestPayloadBkp.class,
            TestPayloadBkp::triggerUserId,
            TestPayloadBkp::setTriggerUserId);

    CaramelField<Collection<String>, TestPayloadBkp> metricNames =
        new SimpleCaramelField<>(
            "metricNames",
            TestPayloadBkp.class,
            TestPayloadBkp::metricNames,
            TestPayloadBkp::setMetricNames);

    CaramelField<Boolean, TestPayloadBkp> isEnableValidation =
        new SimpleCaramelField<>(
            "isEnableValidation",
            TestPayloadBkp.class,
            TestPayloadBkp::isEnableValidation,
            TestPayloadBkp::setIsEnableValidation);
    CaramelField<String, TestPayloadBkp> string =
        new SimpleCaramelField<>(
            "string", TestPayloadBkp.class, TestPayloadBkp::string, TestPayloadBkp::setString);

    CaramelField<String, TestPayloadBkp> secondString =
        new SimpleCaramelField<>(
            "secondString",
            TestPayloadBkp.class,
            TestPayloadBkp::secondString,
            TestPayloadBkp::setSecondString);
    CaramelField<TransformedProduct, TestPayloadBkp> nextProduct =
        new SimpleCaramelField<>(
            "nextProduct",
            TestPayloadBkp.class,
            TestPayloadBkp::nextProduct,
            TestPayloadBkp::setNextProduct);
    CaramelField<Collection<Metric>, TestPayloadBkp> metrics =
        new SimpleCaramelField<>(
            "metrics", TestPayloadBkp.class, TestPayloadBkp::metrics, TestPayloadBkp::setMetrics);
  }

  @NotOnlyInitialized private final Value<String, TestPayloadBkp> x1String;

  @NotOnlyInitialized
  private final Value<ProductUpdateEventsContainer, TestPayloadBkp> productUpdateEvents;

  @NotOnlyInitialized
  private final Value<TransformedProduct, TestPayloadBkp> initialTransformedProduct;

  @NotOnlyInitialized private final Value<ProductUpdateEvent, TestPayloadBkp> initProductEvent;

  @NotOnlyInitialized
  private final Value<List<TransformedProduct>, TestPayloadBkp> conditionalTransformedProducts;

  @NotOnlyInitialized private final Value<String, TestPayloadBkp> triggerUserId;
  @NotOnlyInitialized private final Value<Collection<Metric>, TestPayloadBkp> _metrics;
  @NotOnlyInitialized private final Value<Collection<String>, TestPayloadBkp> metricNames;
  @NotOnlyInitialized private final Value<Boolean, TestPayloadBkp> isEnableValidation;
  @NotOnlyInitialized private final Value<String, TestPayloadBkp> string;
  @NotOnlyInitialized private final Value<String, TestPayloadBkp> secondString;
  @NotOnlyInitialized private final Value<TransformedProduct, TestPayloadBkp> nextProduct;

  @Override
  public Collection<Metric> metrics() {
    return _metrics.getOrThrow();
  }

  public void setMetrics(Collection<Metric> metrics) {
    this._metrics.set(metrics);
  }
  /* ---------- Collection<Metric> metrics - END -----------*/

  public String x1String() {
    return x1String.get().orElseThrow();
  }

  public void setX1String(String string) {
    x1String.set(string);
  }

  @Override
  public ProductUpdateEventsContainer productUpdateEvents() {
    return productUpdateEvents.get().orElseThrow();
  }

  public void setProductUpdateEvents(ProductUpdateEventsContainer productUpdateEvents) {
    this.productUpdateEvents.set(productUpdateEvents);
  }

  @Override
  public TransformedProduct initialTransformedProduct() {
    return initialTransformedProduct.get().orElseThrow();
  }

  public void setInitialTransformedProduct(TransformedProduct transformedProduct) {
    this.initialTransformedProduct.set(transformedProduct);
  }

  @Override
  public ProductUpdateEvent initProductEvent() {
    return initProductEvent.get().orElseThrow();
  }

  public void setInitProductEvent(ProductUpdateEvent productEvent) {
    this.initProductEvent.set(productEvent);
  }

  public List<TransformedProduct> conditionalTransformedProducts() {
    return conditionalTransformedProducts.get().orElseThrow();
  }

  public void setConditionalTransformedProducts(List<TransformedProduct> transformedProducts) {
    this.conditionalTransformedProducts.set(transformedProducts);
  }

  @Override
  public String triggerUserId() {
    return triggerUserId
        .get()
        .orElseThrow(() -> new AccessBeforeInitializationException(triggerUserId));
  }

  public void setTriggerUserId(String triggerUserId) {
    this.triggerUserId.set(triggerUserId);
  }

  @Override
  public Collection<String> metricNames() {
    return metricNames.getOrThrow();
  }

  public void setMetricNames(Collection<String> metrics) {
    this.metricNames.set(metrics);
  }

  @Override
  public boolean isEnableValidation() {
    return isEnableValidation.getOrThrow();
  }

  public void setIsEnableValidation(boolean isEnableValidation) {
    this.isEnableValidation.set(isEnableValidation);
  }

  @Override
  public String string() {
    return string.getOrThrow();
  }

  public void setString(String s) {
    string.set(s);
  }

  @Override
  public String secondString() {
    return secondString.getOrThrow();
  }

  public void setSecondString(String s) {
    secondString.set(s);
  }

  @Override
  public TransformedProduct nextProduct() {
    return nextProduct.getOrThrow();
  }

  public void setNextProduct(TransformedProduct transformedProduct) {
    this.nextProduct.set(transformedProduct);
  }
}