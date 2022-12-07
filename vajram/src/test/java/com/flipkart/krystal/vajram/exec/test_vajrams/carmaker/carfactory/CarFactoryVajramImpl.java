package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.carfactory;

import com.flipkart.krystal.krystex.Result;
import com.flipkart.krystal.krystex.SingleResult;
import com.flipkart.krystal.vajram.ExecutionContext;
import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory.Chassis;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory.ChassisColorRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory.ChassisDashboardRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.Dashboard;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.DashboardRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.Tyre;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.TyreRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.concurrent.ExecutionException;

// Auto generated and managed by Krystal
public class CarFactoryVajramImpl extends CarFactoryVajram {

  @Override
  public ImmutableList<String> executeNonBlocking(ExecutionContext executionContext) {
    try {
      Optional<String> carBrandStr = executionContext.<Optional<String>>getValue("car_brand");
      CarBrandRequest carBrandRequest = new CarBrandRequest(carBrandStr.get());
      Chassis chassis = executionContext.<Chassis>getValue("chassis");
      Tyre tyre = executionContext.<Tyre>getValue("tyre");

      return ImmutableList.of(makeCar(
          new InputUtils.AllInputs(carBrandRequest, tyre, chassis)));
    } catch (Exception e) {
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
          try {
            Optional<String> tyreBrand = executionContext.<Optional<String>>getValue("tyre_request");
            return ImmutableList.of(TyreRequest.builder().brandName(tyreBrand.get()).count(3).size(15));
          } catch (Exception e) {
            String str = e.toString();
          }
        }
      }
      case "chassis" -> {
        if (Set.of("dashboard").equals(resolvableInputs)) {
          try {
            Dashboard dashboard = executionContext.<Dashboard>getValue("dashboard");
            return ImmutableList.of(ChassisDashboardRequest.builder().dashboard(dashboard));
          } catch (Exception e) {
            String str = e.toString();

          }
        }
        if (Set.of("chassis_color").equals(resolvableInputs)) {
          try {
            Optional<String> chassisColor = executionContext.<Optional<String>>getValue("chassis_color");
            return ImmutableList.of(ChassisColorRequest.builder().color(chassisColor.get()));
          } catch (Exception e) {
            String str = e.toString();

          }
        }
      }
      case "dashboard" -> {
        if (Set.of("driving_side").equals(resolvableInputs)) {
          try {
            Optional<String> drivingSide = executionContext.<Optional<String>>getValue("driving_side");
            return ImmutableList.of(DashboardRequest.builder().drivingSide(drivingSide.get()));
          } catch (Exception e) {
            String str = e.toString();
          }
        }
      }
    }
    throw new RuntimeException();
  }
}
