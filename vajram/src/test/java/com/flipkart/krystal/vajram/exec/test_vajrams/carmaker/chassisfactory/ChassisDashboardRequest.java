package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.Dashboard;
import com.google.common.collect.ImmutableMap;

import java.util.Optional;

public record ChassisDashboardRequest(Dashboard dashboard) implements VajramRequest {
    @Override
    public ImmutableMap<String, Optional<Object>> asMap() {
        return ImmutableMap.<String, Optional<Object>>builder()
                .put("dashboard", Optional.ofNullable(dashboard()))
                .build();
    }

    public static ChassisDashboardRequest.Builder builder() {
        return new ChassisDashboardRequest.Builder();
    }

    public static class Builder implements RequestBuilder<ChassisDashboardRequest> {

        private Dashboard dashboard;

        ChassisDashboardRequest.Builder ChassisDashboardRequest(Dashboard dashboard) {
            this.dashboard = dashboard;
            return this;
        }

        public Builder dashboard(Dashboard dashboard) {
            this.dashboard = dashboard;
            return this;
        }

        @Override
        public ChassisDashboardRequest build() {
            return new ChassisDashboardRequest(dashboard);
        }

        private Builder() {}
    }
}
