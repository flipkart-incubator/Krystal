package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory;

public class InputUtils {
    record AllInputs(DashboardRequest dashboardRequest) {
        public String drivingSide() {
            return dashboardRequest.drivingSide();
        }
    }
}
