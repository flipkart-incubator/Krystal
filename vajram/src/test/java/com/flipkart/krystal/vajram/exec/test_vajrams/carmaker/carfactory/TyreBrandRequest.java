package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.carfactory;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public record TyreBrandRequest(String tyreBrand) implements VajramRequest {

    @Override
    public ImmutableMap<String, Optional<Object>> asMap() {
        return ImmutableMap.<String, Optional<Object>>builder()
                .put("tyre_brand", Optional.ofNullable(tyreBrand()))
                .build();
    }

    static class Builder implements RequestBuilder<TyreBrandRequest> {

        private String tyreBrand;

        Builder CarRequest(String tyreBrand) {
            this.tyreBrand = tyreBrand;
            return this;
        }

        public Builder tyreBrand(String tyreBrand) {
            this.tyreBrand = tyreBrand;
            return this;
        }

        @Override
        public TyreBrandRequest build() {
            return new TyreBrandRequest(tyreBrand);
        }

        private Builder() {}
    }
}
