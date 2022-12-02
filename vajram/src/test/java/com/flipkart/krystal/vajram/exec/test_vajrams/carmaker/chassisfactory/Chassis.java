package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory;

import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.CarPart;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.Dashboard;

public class Chassis implements CarPart {
    private String color;
    private Dashboard dashboard;

    public Chassis(String color, Dashboard dashboard) {
        this.color = color;
        this.dashboard = dashboard;
    }

    public String color() {return this.color;}
    public Dashboard dashboard() {return this.dashboard;}

    @Override
    public String getDetails() {
        return "Chassis: color - %s | %s with Dashboard".formatted(color(), dashboard.getDetails());
    }
}
