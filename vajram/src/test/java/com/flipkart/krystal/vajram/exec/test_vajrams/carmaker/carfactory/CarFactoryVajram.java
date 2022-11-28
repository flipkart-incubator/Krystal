package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.carfactory;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.carfactory.CarFactoryVajram.ID;

import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
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
                    // Local name for this input
                    .name("car_brand")
                    // Data type - used for code generation
                    .type(string())
                    // If this input is not provided by the client, throw a build time error.
                    .mandatory()
                    .build(),
                Input.builder()
                    // Local name for this input
                    .name("tyre_brand")
                    // Data type - used for code generation
                    .type(string())
                    // If this input is not provided by the client, throw a build time error.
                    .mandatory()
                    .build(),
                Dependency.builder()
                    .name("tyre")
                    .dataAccessSpec(vajramID(TyreFactoryVajram.ID))
                    .isMandatory()
                    .build());
    }

    @Override
    public ImmutableList<InputResolver> getSimpleInputResolvers() {
        return ImmutableList.of(new ForwardingResolver(
                "tyre_brand",
                "tyre",
                "tyre_request",
                (brand) -> new TyreRequest((String) brand, 4, 14)));
    }

    @Resolve(value = "tyre", inputs = "tyre_request")
    public TyreRequest tyreForTyreFactory(@BindFrom("tyre_brand") String brandName) {
        return new TyreRequest(brandName, 4, 14);
    }

    @VajramLogic
    public String makeCar(InputUtils.AllInputs inputs) {
        return "Car of comes with tyres of %s".formatted(inputs.tyre().message);
    }
}

