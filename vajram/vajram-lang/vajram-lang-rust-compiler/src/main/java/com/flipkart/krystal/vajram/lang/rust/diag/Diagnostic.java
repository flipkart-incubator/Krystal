package com.flipkart.krystal.vajram.lang.rust.diag;

import com.flipkart.krystal.vajram.lang.rust.ast.SourceLocation;

/** A structured compiler message, replacing the ad-hoc string-list used in grammar-only tests. */
public record Diagnostic(Severity severity, SourceLocation location, String message) {

  public enum Severity {
    ERROR,
    WARNING
  }

  public static Diagnostic error(SourceLocation location, String message) {
    return new Diagnostic(Severity.ERROR, location, message);
  }

  public static Diagnostic warning(SourceLocation location, String message) {
    return new Diagnostic(Severity.WARNING, location, message);
  }

  @Override
  public String toString() {
    return severity + " " + location + ": " + message;
  }
}
