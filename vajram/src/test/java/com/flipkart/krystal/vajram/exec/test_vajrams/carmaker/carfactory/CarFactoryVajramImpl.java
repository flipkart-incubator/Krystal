package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.carfactory;

import com.flipkart.krystal.vajram.ExecutionContext;
import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.Tyre;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.TyreRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;

// Auto generated and managed by Krystal
public class CarFactoryVajramImpl extends CarFactoryVajram {

    @Override
    public ImmutableList<String> executeNonBlocking(ExecutionContext executionContext) {
        try {
            CarBrandRequest _request = new CarBrandRequest(executionContext.<Optional<String>>getValue("car_brand").get());
            Tyre tyre = executionContext.<Tyre>getValue("tyre");
            return ImmutableList.of(makeCar(
                    new InputUtils.AllInputs(_request, tyre)));
        }catch (Exception e) {
            String err = e.toString();
        }
        return null;
    }

    @Override
    public ImmutableList<RequestBuilder<?>> resolveInputOfDependency(
            String dependency, ImmutableSet<String> resolvableInputs, ExecutionContext executionContext) {
            switch (dependency) {
                case "tyre" -> {
                    if (Set.of("tyre_request").equals(resolvableInputs)) {
                        String tyreBrand = executionContext.<Optional<String>>getValue("tyre_brand").get();
                        return ImmutableList.of(TyreRequest.builder().brandName(tyreBrand).count(3).size(15));
                    }
                }
            }
            throw new RuntimeException();
    }
}
