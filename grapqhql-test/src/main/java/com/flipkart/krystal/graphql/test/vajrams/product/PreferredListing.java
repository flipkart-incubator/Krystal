package com.flipkart.krystal.graphql.test.vajrams.product;

import com.flipkart.krystal.graphql.test.EntityFieldMapping;
import com.flipkart.krystal.graphql.test.models.Listing;
import com.flipkart.krystal.graphql.test.models.Product;
import com.flipkart.krystal.graphql.test.models.Product.Fields;
import com.flipkart.krystal.graphql.test.productpage.ProductPageRequestContext;
import com.flipkart.krystal.graphql.test.vajrams.product.PreferredListingInputUtil.AllInputs;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;

@VajramDef(PreferredListing.ID)
public abstract class PreferredListing extends ComputeVajram<Listing> {
  public static final String ID = "PreferredListing";

  @VajramLogic
  public static Listing getPreferredListing(AllInputs allInputs) {
    return new Listing()
        .listingId(allInputs.productId() + allInputs.preferredListingId().orElse(":LISTING_1"));
  }

  @EntityFieldMapping(type = Product.TYPE, fieldNames = Fields.preferred_listing)
  public static PreferredListingRequest requestFromPojo(
      ProductPageRequestContext requestContext, Product container) {
    return PreferredListingRequest.builder()
        .productId(container.productId())
        .preferredListingId(requestContext.preferredListingId())
        .build();
  }
}
