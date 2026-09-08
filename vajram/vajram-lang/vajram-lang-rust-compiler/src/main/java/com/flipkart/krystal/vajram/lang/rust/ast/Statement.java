package com.flipkart.krystal.vajram.lang.rust.ast;

/** Grammar rule {@code statement}: either a facet assignment or a throw. */
public sealed interface Statement {

  record Assign(InputDecl decl, Expr value) implements Statement {}

  record Throw(Expr value) implements Statement {}
}
