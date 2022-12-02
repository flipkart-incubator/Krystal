package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker;

import static com.flipkart.krystal.vajram.exec.VajramGraph.loadFromClasspath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.flipkart.krystal.vajram.exec.KrystexVajramExecutor;
import com.flipkart.krystal.vajram.exec.VajramGraph;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.carfactory.CarFactoryVajram;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

class CarFactoryTests {
    @Test
    void  requestExecution_vajramWithDependencies_success()
            throws ExecutionException, InterruptedException, TimeoutException {
        VajramGraph vajramGraph =
                loadFromClasspath("com.flipkart.krystal.vajram.exec.test_vajrams");
        try (KrystexVajramExecutor krystexVajramExecutor =
                     new KrystexVajramExecutor(vajramGraph, "qwerty")) {
            CompletableFuture<String> result =
                    krystexVajramExecutor.requestExecutionWithInputs(CarFactoryVajram.ID,
                            ImmutableMap.<String, Optional<Object>>builder()
                            .put("car_brand", Optional.ofNullable("Toyota"))
                            .put("car_color", Optional.ofNullable("Red"))
                            .put("driving_side", Optional.ofNullable("left"))
                            .put("tyre_brand", Optional.ofNullable("MRF")).build());
            assertNotEquals("Received following parts at Toyota car factory\n"
                    + "Tyres: Brand - MRF | Count - 3 | size 15", result.get(5, TimeUnit.HOURS));
            result.thenApply(abc -> {
                return null;
            });
        }
    }
}
