package com.flipkart.krystal.graphql.test.datafetchers;

import com.flipkart.krystal.graphql.test.AbstractGraphQLModel;
import com.flipkart.krystal.graphql.test.models.Listing;
import com.flipkart.krystal.graphql.test.models.Product;
import com.flipkart.krystal.graphql.test.models.Product.Fields;
import com.flipkart.krystal.graphql.test.productpage.ProductPageRequestContext;
import com.flipkart.krystal.graphql.test.vajrams.listing.ListingPrice;
import com.flipkart.krystal.graphql.test.vajrams.product.PreferredListing;
import com.flipkart.krystal.graphql.test.vajrams.product.ProductBaseInfo;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.VajramRequest;

// Auto-generated
public class MapperUtil {
  public static VajramRequest mapRequest(
      ApplicationRequestContext rc, AbstractGraphQLModel model, String fieldName) {
    if (rc instanceof ProductPageRequestContext pprc) {
      if (model instanceof Product p) {
        if (Fields.preferred_listing.equals(fieldName)) {
          return PreferredListing.requestFromPojo(pprc, p);
        }
        return ProductBaseInfo.requestFromPojo(pprc, p);
      }
      if (model instanceof Listing l) {
        if (Listing.Fields.price.equals(fieldName)) {
          return ListingPrice.toRequest(pprc, l);
        }
      }
    }
    throw new UnsupportedOperationException();
  }

  public static Object mapResponse(AbstractGraphQLModel source, String fieldName, Object response) {
    if (source instanceof Product) {
      if (Fields.preferred_listing.equals(fieldName)) {
        return response;
      }
    }
    if (source instanceof Listing l) {
      if (Listing.Fields.price.equals(fieldName)) {
        return response;
      }
    }
    if (response instanceof AbstractGraphQLModel model) {
      return model.get(fieldName);
    }
    throw new UnsupportedOperationException();
  }

  public static Object mapStatic(
      ApplicationRequestContext applicationRequestContext,
      AbstractGraphQLModel source,
      String fieldName) {
    if (applicationRequestContext instanceof ProductPageRequestContext productPageRequestContext) {
      if ("primary_product".equals(fieldName)) {
        return new Product().productId(productPageRequestContext.primaryProductId());
      }
    }
    return null;
  }
}
