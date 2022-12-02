package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.dashboardfactory.Dashboard;

public final class DashboardType implements DataType<Dashboard> {

    private static final DashboardType INSTANCE = new DashboardType();

    public static DashboardType dashboard() {
        return INSTANCE;
    }

    private DashboardType() {}
}