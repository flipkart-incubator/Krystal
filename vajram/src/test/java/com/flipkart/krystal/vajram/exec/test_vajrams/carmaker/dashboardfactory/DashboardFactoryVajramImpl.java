package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory;

import com.flipkart.krystal.vajram.ExecutionContext;
import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory.Chassis;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.DashboardRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory.ChassisFactoryVajram;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.InputUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

// Auto generated and managed by Krystal
public class DashboardFactoryVajramImpl extends DashboardFactoryVajram {

    @Override
    public ImmutableList<Dashboard> executeNonBlocking(ExecutionContext executionContext) {
        Object requestValue = executionContext.getValue("driving_side");
        DashboardRequest dashboardRequest = null;
        if (requestValue instanceof DashboardRequest.Builder builder) {
            dashboardRequest = builder.build();
        } else {
            throw new IllegalArgumentException(
                    "Unsupported type %s for input %s in vajram %s"
                            .formatted(dashboardRequest.getClass(), "driving side", "DashboardFactoryVajram"));
        }
        InputUtils.AllInputs allInputs = new InputUtils.AllInputs(
                new DashboardRequest(dashboardRequest.drivingSide()));
        return ImmutableList.of(super.makeDashboard(allInputs));
    }

    @Override
    public ImmutableList<RequestBuilder<?>> resolveInputOfDependency(
            String dependency, ImmutableSet<String> resolvableInputs, ExecutionContext executionContext) {
        throw new UnsupportedOperationException();
    }
}
