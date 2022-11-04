package com.flipkart.krystal.vajram.samples.greeting;

import com.flipkart.krystal.vajram.RequestBuilder;

// Auto-generated and managed by Krystal
public record UserServiceVajramRequest(String userId) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder implements RequestBuilder<UserServiceVajramRequest> {

    private String userId;

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    @Override
    public UserServiceVajramRequest build() {
      return new UserServiceVajramRequest(this.userId);
    }

    private Builder() {}
  }
}
