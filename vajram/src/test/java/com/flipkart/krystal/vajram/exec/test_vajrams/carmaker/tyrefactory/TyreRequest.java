package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public record TyreRequest(String brandName, Integer count, Integer size) implements VajramRequest {

    @Override
    public ImmutableMap<String, Optional<Object>> asMap() {
        return ImmutableMap.<String, Optional<Object>>builder()
                .put("brandName", Optional.ofNullable(brandName))
                .put("count", Optional.ofNullable(count))
                .put("size", Optional.ofNullable(size))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements RequestBuilder<TyreRequest> {

        private String brandName;
        private Integer count;
        private Integer size;

        public Builder brandName(String brandName) {
            this.brandName = brandName;
            return this;
        }

        public Builder count(Integer count) {
            this.count = count;
            return this;
        }

        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        @Override
        public TyreRequest build() {
            return new TyreRequest(brandName, count, size);
        }

        private Builder() {}
    }
}

