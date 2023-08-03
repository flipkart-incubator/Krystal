package com.flipkart.krystal.mojo;

public enum PublishTarget {
  LOCAL("L"),
  REMOTE("R");

  private final String shortString;

  PublishTarget(String shortString) {
    this.shortString = shortString;
  }

  String shortString() {
    return shortString;
  }
}
