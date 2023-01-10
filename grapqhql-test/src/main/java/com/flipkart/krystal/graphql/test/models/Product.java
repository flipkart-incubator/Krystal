package com.flipkart.krystal.graphql.test.models;

import com.flipkart.krystal.graphql.test.AbstractGraphQLModel;

// Auto-generated
public class Product extends AbstractGraphQLModel {

  public static final String TYPE = "Product";

  public String productId() {
    return (String) this._values.get("product_id");
  }

  public String title() {
    return (String) this._values.get("product_id");
  }

  public String subtitle() {
    return (String) this._values.get("product_id");
  }

  public Product productId(String productId) {
    this._values.put("product_id", productId);
    return this;
  }

  public Product title(String title) {
    this._values.put("title", title);
    return this;
  }

  public Product subTitle(String subTitle) {
    this._values.put("sub_title", subTitle);
    return this;
  }

  public static final class Fields {
    public static final String product_id = "product_id";
    public static final String title = "title";
    public static final String sub_title = "sub_title";
    public static final String rating = "rating";
    public static final String preferred_listing = "preferred_listing";
  }
}
