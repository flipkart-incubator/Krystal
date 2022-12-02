package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.DashboardRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.TyreRequest;
import com.google.common.collect.ImmutableMap;

import java.util.Optional;

public record ChassisColorRequest(String color) implements VajramRequest {
    @Override
    public ImmutableMap<String, Optional<Object>> asMap() {
        return ImmutableMap.<String, Optional<Object>>builder()
                .put("color", Optional.ofNullable(color()))
                .build();
    }

    public static ChassisColorRequest.Builder builder() {
        return new ChassisColorRequest.Builder();
    }

    public static class Builder implements RequestBuilder<ChassisColorRequest> {

        private String color;

        ChassisColorRequest.Builder ChassisColorRequest(String color) {
            this.color = color;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        @Override
        public ChassisColorRequest build() {
            return new ChassisColorRequest(color);
        }

        private Builder() {}
    }
}
