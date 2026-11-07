package com.flipkart.krystal.keka.rust.ast;

/** A position in a {@code .keka} source file, used for diagnostics. */
public record SourceLocation(String file, int line, int column) {

  @Override
  public String toString() {
    return file + ':' + line + ':' + column;
  }
}
