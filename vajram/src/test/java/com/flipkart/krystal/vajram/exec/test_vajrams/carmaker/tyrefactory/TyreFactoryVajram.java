package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory;

import com.flipkart.krystal.vajram.BlockingVajram;
import com.flipkart.krystal.vajram.DefaultModulatedBlockingVajram;
import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.UnmodulatedAsyncVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.InputUtils.AllInputs;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.TyreFactoryVajram.ID;

@VajramDef(ID)
public abstract class TyreFactoryVajram extends DefaultModulatedBlockingVajram<Tyre> {

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
    public CompletableFuture<Tyre> makeTyre(AllInputs inputs) {
        Tyre tyre = new Tyre(inputs.brandName(), inputs.count(), inputs.size());
        Executor delayed = CompletableFuture.delayedExecutor(10L, TimeUnit.SECONDS);
        CompletableFuture<Tyre> cTyre = CompletableFuture.supplyAsync(() -> tyre, delayed);
        return cTyre;
    }
}
