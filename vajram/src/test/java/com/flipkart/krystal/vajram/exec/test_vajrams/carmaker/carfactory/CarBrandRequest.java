package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.carfactory;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public record CarBrandRequest(String carBrand) implements VajramRequest {

    @Override
    public ImmutableMap<String, Optional<Object>> asMap() {
        return ImmutableMap.<String, Optional<Object>>builder()
                .put("tyre_brand", Optional.ofNullable(carBrand()))
                .build();
    }

    static class Builder implements RequestBuilder<CarBrandRequest> {

        private String carBrand;

        Builder CarRequest(String carBrand) {
            this.carBrand = carBrand;
            return this;
        }

        public Builder carBrand(String carBrand) {
            this.carBrand = carBrand;
            return this;
        }

        @Override
        public CarBrandRequest build() {
            return new CarBrandRequest(carBrand);
        }

        private Builder() {}
    }
}
