package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory;

import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.CarPart;

public class Dashboard implements CarPart {
    public String drivingSide;

    public Dashboard(String drivingSide) {
        this.drivingSide = drivingSide;
    }

    public String drivingSide() {
        return drivingSide;
    }

    public String getDetails() {
        return "Dashboard: Driving side - %s".formatted(drivingSide);
    }
}
