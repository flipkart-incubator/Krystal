package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.data.Inputs;

public class InvalidRequestException extends RuntimeException {
  public InvalidRequestException(Inputs invalidInputs) {
    super("Invalid inputs for the vajram");
  }
}
