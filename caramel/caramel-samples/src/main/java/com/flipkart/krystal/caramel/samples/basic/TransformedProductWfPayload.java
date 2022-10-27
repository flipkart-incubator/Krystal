package com.flipkart.krystal.caramel.samples.basic;

import com.flipkart.krystal.caramel.model.AbstractValue;
import com.flipkart.krystal.caramel.model.Field;
import com.flipkart.krystal.caramel.model.SimpleField;

public class TransformedProductWfPayload implements TransformedProductWfPayloadDefinition {
  public interface TransformedProductWfFields {
    Field<ProductUpdateEvent, TransformedProductWfPayload> productUpdateEvent =
        new SimpleField<>(
            "productUpdateEvent",
            TransformedProductWfPayload.class,
            TransformedProductWfPayload::productUpdateEvent,
            TransformedProductWfPayload::setProductUpdateEvent);

    Field<TransformedProduct, TransformedProductWfPayload> transformedProduct =
        new SimpleField<>(
            "transformedProduct",
            TransformedProductWfPayload.class,
            TransformedProductWfPayload::transformedProduct,
            TransformedProductWfPayload::setTransformedProduct);
  }

  private final Value<ProductUpdateEvent> productUpdateEvent =
      new Value<>(TransformedProductWfFields.productUpdateEvent);

  private final Value<TransformedProduct> transformedProduct =
      new Value<>(TransformedProductWfFields.transformedProduct);

  @Override
  public ProductUpdateEvent productUpdateEvent() {
    return productUpdateEvent.getOrThrow();
  }

  public void setProductUpdateEvent(ProductUpdateEvent productUpdateEvent) {
    this.productUpdateEvent.set(productUpdateEvent);
  }

  @Override
  public TransformedProduct transformedProduct() {
    return transformedProduct.getOrThrow();
  }

  public void setTransformedProduct(TransformedProduct transformedProduct) {
    this.transformedProduct.set(transformedProduct);
  }

  private final class Value<T> extends AbstractValue<T, TransformedProductWfPayload> {

    private final Field<T, TransformedProductWfPayload> field;

    public Value(Field<T, TransformedProductWfPayload> field) {
      this.field = field;
    }

    @Override
    public Field<T, TransformedProductWfPayload> field() {
      return field;
    }

    @Override
    public TransformedProductWfPayload getPayload() {
      return TransformedProductWfPayload.this;
    }
  }
}
