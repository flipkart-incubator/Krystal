package com.flipkart.krystal.vajram.samples.greeting;

import com.flipkart.krystal.vajram.RequestBuilder;

// Auto-generated and managed by Krystal
public record GreetingVajramRequest(String userId) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder implements RequestBuilder<GreetingVajramRequest> {

    private String userId;

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    @Override
    public GreetingVajramRequest build() {
      return new GreetingVajramRequest(this.userId);
    }

    private Builder() {}
  }
}
