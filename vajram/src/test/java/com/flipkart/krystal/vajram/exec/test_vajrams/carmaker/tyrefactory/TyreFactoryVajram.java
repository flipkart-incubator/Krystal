package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory;

import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.InputUtils.AllInputs;

import java.util.List;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.TyreFactoryVajram.ID;

@VajramDef(ID)
public abstract class TyreFactoryVajram extends NonBlockingVajram<Tyre> {

    public static final String ID = "flipkart.krystal.test_vajrams.tyre_factory";

    @Override
    public List<VajramInputDefinition> getInputDefinitions() {
        return List.of(Input.builder()
                        .name("tyre_request")
                        .type(string())
                        .mandatory()
                        .build());
    }

    @VajramLogic
    public Tyre makeTyre(AllInputs inputs) {
        return new Tyre("Created tyres of brand %s".formatted(inputs.brandName(), inputs.size(), inputs.count()));
    }
}
