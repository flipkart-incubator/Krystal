package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.carfactory;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.carfactory.CarFactoryVajram.ID;

import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory.ChassisColorRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory.ChassisDashboardRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory.ChassisFactoryVajram;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory.ChassisFactoryVajramImpl;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.Dashboard;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.DashboardFactoryVajram;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.DashboardRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.TyreFactoryVajram;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.TyreRequest;
import com.flipkart.krystal.vajram.inputs.*;
import com.google.common.collect.ImmutableList;

import java.util.List;

@VajramDef(ID)
public abstract class CarFactoryVajram extends NonBlockingVajram<String> {

    public static final String ID = "flipkart.krystal.test_vajrams.CarFactory";

    @Override
    public List<VajramInputDefinition> getInputDefinitions() {
        return List.of(Input.builder()
                    .name("car_brand")
                    .type(string())
                    .mandatory()
                    .build(),
                Input.builder()
                    .name("tyre_brand")
                    .type(string())
                    .mandatory()
                    .build(),
                Input.builder()
                    .name("car_color")
                    .type(string())
                    .mandatory()
                    .build(),
                Input.builder()
                        .name("driving_side")
                        .type(string())
                        .mandatory()
                        .build(),
                /*
                Dependency.builder()
                    .name("tyre")
                    .dataAccessSpec(vajramID(TyreFactoryVajram.ID))
                    .isMandatory()
                    .build(),

                 */
                Dependency.builder()
                    .name("chassis")
                    .dataAccessSpec(vajramID(ChassisFactoryVajram.ID))
                    .isMandatory()
                    .build(),
                Dependency.builder()
                    .name("dashboard")
                    .dataAccessSpec(vajramID(DashboardFactoryVajram.ID))
                    .isMandatory()
                    .build());
    }

    @Override
    public ImmutableList<InputResolver> getSimpleInputResolvers() {
        return ImmutableList.of(
            /*
            new ForwardingResolver(
                "tyre_brand",
                "tyre",
                "tyre_request",
                (brand) -> new TyreRequest((String) brand, 4, 14)),

             */
            new ForwardingResolver(
                "driving_side",
                "dashboard",
                "driving_side",
                (brand) -> new DashboardRequest("left")),
            new ForwardingResolver(
                "car_color",
                "chassis",
                "chassis_color",
                (color) -> new ChassisColorRequest("red")),
            new ForwardingResolver(
                "dashboard",
                "chassis",
                "dashboard",
                (dashboard) -> new ChassisDashboardRequest((Dashboard)dashboard))
        );
    }

    @VajramLogic
    public String makeCar(InputUtils.AllInputs inputs) {
        String str = "Received following parts at %s car factory\n%s".formatted(inputs.carBrand(),
            inputs.chassis().getDetails());
        return str;
    }
}

