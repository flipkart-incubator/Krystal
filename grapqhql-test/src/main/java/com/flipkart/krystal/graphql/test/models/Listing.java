package com.flipkart.krystal.graphql.test.models;

import static com.flipkart.krystal.graphql.test.models.Listing.Fields.listing_id;

import com.flipkart.krystal.graphql.test.AbstractGraphQLModel;

// Auto-generated
public final class Listing extends AbstractGraphQLModel {

  public static final String TYPE = "Listing";

  public Listing listingId(String listingId) {
    this._values.put("listing_id", listingId);
    return this;
  }

  public Listing price(double price) {
    this._values.put("price", price);
    return this;
  }

  public String listingId() {
    return (String) this._values.get(listing_id);
  }

  public static final class Fields {
    public static final String listing_id = "listing_id";
    public static final String price = "price";

    private Fields() {}
  }
}
