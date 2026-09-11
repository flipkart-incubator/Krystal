package com.flipkart.krystal.vajram.lang.rust.ast;

/** A position in a {@code .vajram} source file, used for diagnostics. */
public record SourceLocation(String file, int line, int column) {

  @Override
  public String toString() {
    return file + ':' + line + ':' + column;
  }
}
