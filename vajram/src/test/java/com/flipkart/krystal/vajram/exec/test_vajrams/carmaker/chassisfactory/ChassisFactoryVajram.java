package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory;

import com.flipkart.krystal.vajram.*;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;

import java.util.List;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.DashboardType.dashboard;
import static com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory.ChassisFactoryVajram.ID;


@VajramDef(ID)
public abstract class ChassisFactoryVajram extends NonBlockingVajram<Chassis> {

    public static final String ID = "flipkart.krystal.test_vajrams.chassis_factory";

    @Override
    public List<VajramInputDefinition> getInputDefinitions() {
        return List.of(Input.builder()
                    .name("chassis_color")
                    .type(string())
                    .mandatory()
                    .build(),
                Input.builder()
                    .name("dashboard")
                    .type(dashboard())
                    .mandatory()
                    .build());
    }

    @VajramLogic
    public Chassis makeChassis(InputUtils.AllInputs inputs) {
        return new Chassis(inputs.chassisColorRequest().color(), inputs.dashboard());
    }
}

