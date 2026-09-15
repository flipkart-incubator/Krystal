package com.flipkart.krystal.vajram.lang.rust.ast;

/** Grammar rule {@code statement}: a facet assignment, expression statement, or throw. */
public sealed interface Statement {

  record Assign(InputDecl decl, Expr value) implements Statement {}

  record Expression(Expr value) implements Statement {}

  record Throw(Expr value) implements Statement {}
}
