package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.carfactory;

import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.chassisfactory.Chassis;
import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.Tyre;

class InputUtils {
    record AllInputs(CarBrandRequest carBrandRequest, Tyre tyre, Chassis chassis) {
        public String carBrand() {
            return carBrandRequest.carBrand();
        }
        public Tyre tyre() {
            return tyre;
        }
        public Chassis chassis() {return chassis;}
    }
}