package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.TyreRequest;
import com.google.common.collect.ImmutableMap;

import java.util.Optional;

public record DashboardRequest(String drivingSide) implements VajramRequest {
    @Override
    public ImmutableMap<String, Optional<Object>> asMap() {
        return ImmutableMap.<String, Optional<Object>>builder()
                .put("driving_side", Optional.ofNullable(drivingSide()))
                .build();
    }

    public static DashboardRequest.Builder builder() {
        return new DashboardRequest.Builder();
    }

    public static class Builder implements RequestBuilder<DashboardRequest> {

        private String drivingSide;

        DashboardRequest.Builder DashboardRequest(String drivingSide) {
            this.drivingSide = drivingSide;
            return this;
        }

        public Builder drivingSide(String drivingSide) {
            this.drivingSide = drivingSide;
            return this;
        }

        @Override
        public DashboardRequest build() {
            return new DashboardRequest(drivingSide);
        }

        private Builder() {}
    }
}
