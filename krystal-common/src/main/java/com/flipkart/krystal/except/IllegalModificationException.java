package com.flipkart.krystal.except;

public class IllegalModificationException extends RuntimeException {

  public IllegalModificationException() {
    super("Cannot modify a facet after it has been already set");
  }
}
