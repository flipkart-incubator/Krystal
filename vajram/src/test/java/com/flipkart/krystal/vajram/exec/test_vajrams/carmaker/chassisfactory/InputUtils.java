package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory;

import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.Dashboard;

public class InputUtils {
    record AllInputs(ChassisColorRequest chassisColorRequest, Dashboard dashboard) {
        public ChassisColorRequest chassisColorRequest() {
            return chassisColorRequest;
        }
        public Dashboard dashboard() { return dashboard;}
    }
}