package com.flipkart.krystal.graphql.test.vajrams.product;

import static com.flipkart.krystal.graphql.test.vajrams.product.ProductBaseInfo.ID;

import com.flipkart.krystal.graphql.test.EntityFieldMapping;
import com.flipkart.krystal.graphql.test.models.Product;
import com.flipkart.krystal.graphql.test.productpage.ProductPageRequestContext;
import com.flipkart.krystal.graphql.test.vajrams.product.ProductBaseInfoInputUtil.AllInputs;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import java.util.concurrent.CompletableFuture;

@VajramDef(ID)
public abstract class ProductBaseInfo extends IOVajram<Product> {
  public static final String ID = "ProductBaseInfo";

  @VajramLogic
  public static CompletableFuture<Product> getProductBaseInfo(AllInputs allInputs) {
    String productId = allInputs.productId();
    return CompletableFuture.completedFuture(
        new Product()
            .productId(productId)
            .title(productId + " Product Title")
            .subTitle(productId + " Product Subtitle"));
  }

  @EntityFieldMapping(type = Product.TYPE)
  public static ProductBaseInfoRequest requestFromPojo(
      ProductPageRequestContext requestContext, Product container) {
    return ProductBaseInfoRequest.builder().productId(container.productId()).build();
  }
}
