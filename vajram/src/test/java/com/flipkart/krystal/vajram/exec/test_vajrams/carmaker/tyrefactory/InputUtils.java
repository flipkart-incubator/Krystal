package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory;

// Auto-generated and managed by Krystal
final class InputUtils {
    record AllInputs(TyreRequest _request) {
        public String brandName() {
            return _request().brandName();
        }

        public Integer size() {
            return _request().size();
        }

        public Integer count() {
            return _request.count();
        }
    }
}

