package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory;

import com.flipkart.krystal.vajram.ExecutionContext;
import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.Dashboard;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.DashboardRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Optional;

// Auto generated and managed by Krystal
public class ChassisFactoryVajramImpl extends ChassisFactoryVajram {

    @Override
    public ImmutableList<Chassis> executeNonBlocking(ExecutionContext executionContext) {
        try {
            ChassisColorRequest.Builder chassisColorRequest = executionContext.<ChassisColorRequest.Builder>getValue(
                "chassis_color");
            ChassisDashboardRequest.Builder dashboardRequest = executionContext.<ChassisDashboardRequest.Builder>getValue("dashboard");

            InputUtils.AllInputs allInputs = new InputUtils.AllInputs(
                new ChassisColorRequest(chassisColorRequest.build().color()), dashboardRequest.build().dashboard());
            return ImmutableList.of(super.makeChassis(allInputs));
        }catch (Exception e) {
            String str = e.toString();
        }
        return null;
    }

    @Override
    public ImmutableList<RequestBuilder<?>> resolveInputOfDependency(
            String dependency, ImmutableSet<String> resolvableInputs, ExecutionContext executionContext) {
        throw new UnsupportedOperationException();
    }
}

