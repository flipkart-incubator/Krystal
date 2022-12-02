package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory;

import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;

import java.util.List;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.DashboardFactoryVajram.ID;

@VajramDef(ID)
public abstract class DashboardFactoryVajram extends NonBlockingVajram<Dashboard> {

    public static final String ID = "flipkart.krystal.test_vajrams.dashboard_factory";

    @Override
    public List<VajramInputDefinition> getInputDefinitions() {
        return List.of(Input.builder()
                        .name("driving_side")
                        .type(string())
                        .mandatory()
                        .build());
    }

    @VajramLogic
    public Dashboard makeDashboard(InputUtils.AllInputs inputs) {
        return new Dashboard(inputs.drivingSide());
    }
}