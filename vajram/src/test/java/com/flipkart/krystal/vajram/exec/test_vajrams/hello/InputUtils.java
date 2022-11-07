package com.flipkart.krystal.vajram.exec.test_vajrams.hello;

class InputUtils {
  record AllInputs(HelloRequest _request) {
    String name() {
      return _request().name();
    }
  }

}
