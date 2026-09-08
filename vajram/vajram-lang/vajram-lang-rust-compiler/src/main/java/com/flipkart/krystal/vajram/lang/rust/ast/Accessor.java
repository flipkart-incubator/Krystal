package com.flipkart.krystal.vajram.lang.rust.ast;

/**
 * The connective token(s) vajram-lang uses between a preceding expression and the member/call that
 * follows it: plain {@code .}, the errable operator {@code ?}, the soon (future) operator {@code
 * ~}, or a combination of these. Mirrors grammar rule {@code accessor}.
 */
public enum Accessor {
  DOT,
  ERRABLE,
  SOON,
  SOON_DOT,
  ERRABLE_DOT,
  SOON_ERRABLE_DOT,
  SOON_ERRABLE;

  public boolean hasSoon() {
    return this == SOON || this == SOON_DOT || this == SOON_ERRABLE_DOT || this == SOON_ERRABLE;
  }

  public boolean hasErrable() {
    return this == ERRABLE
        || this == ERRABLE_DOT
        || this == SOON_ERRABLE_DOT
        || this == SOON_ERRABLE;
  }
}
