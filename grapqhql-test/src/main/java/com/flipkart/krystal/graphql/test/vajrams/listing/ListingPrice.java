package com.flipkart.krystal.graphql.test.vajrams.listing;

import com.flipkart.krystal.graphql.test.EntityFieldMapping;
import com.flipkart.krystal.graphql.test.models.Listing;
import com.flipkart.krystal.graphql.test.models.Listing.Fields;
import com.flipkart.krystal.graphql.test.productpage.ProductPageRequestContext;
import com.flipkart.krystal.graphql.test.vajrams.listing.LisitngPriceInputUtil.AllInputs;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import java.util.concurrent.CompletableFuture;

@VajramDef(ListingPrice.ID)
public abstract class ListingPrice extends IOVajram<Double> {

  public static final String ID = "ListingPrice";

  @VajramLogic
  public static CompletableFuture<Double> getPrice(AllInputs allInputs) {
    return CompletableFuture.completedFuture(100.0);
  }

  @EntityFieldMapping(type = Listing.TYPE, fieldNames = Fields.price)
  public static LisitngPriceRequest toRequest(ProductPageRequestContext rc, Listing model) {
    return LisitngPriceRequest.builder().listingId(model.listingId()).build();
  }
}
