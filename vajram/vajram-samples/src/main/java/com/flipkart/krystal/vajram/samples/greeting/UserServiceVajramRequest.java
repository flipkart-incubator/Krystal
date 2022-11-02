package com.flipkart.krystal.vajram.samples.greeting;

// Auto-generated and managed by Krystal
public record UserServiceVajramRequest(String userId) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private Builder() {}

    private String userId;

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }
  }
}
