package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.carfactory;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;

import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.Tyre;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public record TyreRequest(Tyre tyre) implements VajramRequest {

    @Override
    public ImmutableMap<String, Optional<Object>> asMap() {
        return ImmutableMap.<String, Optional<Object>>builder()
                .put("tyre", Optional.ofNullable(tyre()))
                .build();
    }

    static class Builder implements RequestBuilder<TyreRequest> {

        private Tyre tyre;

        Builder Tyre(Tyre tyre) {
            this.tyre = tyre;
            return this;
        }

        public Builder tyre(Tyre tyre) {
            this.tyre = tyre;
            return this;
        }

        @Override
        public TyreRequest build() {
            return new TyreRequest(tyre);
        }

        private Builder() {}
    }
}

