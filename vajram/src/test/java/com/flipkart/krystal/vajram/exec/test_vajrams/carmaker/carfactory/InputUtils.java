package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.carfactory;

import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory.Tyre;

class InputUtils {
    record AllInputs(CarBrandRequest carBrandRequest, Tyre tyre) {
        public String carBrand() {
            return carBrandRequest.carBrand();
        }
        public Tyre tyre() {
            return tyre;
        }
    }
}