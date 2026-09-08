package com.flipkart.krystal.vajram.lang.rust.ast;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * vajram-lang expression AST (grammar rule {@code expr} and the rules it composes: {@code
 * func_chain}, {@code func_call}/{@code func_call_in_output_logic}, {@code accessor}).
 *
 * <p>Per the confirmed compiler strategy, this is intentionally a thin, structural AST: it
 * preserves just enough shape to (a) desugar vajram-lang's own operators ({@code ?}, {@code ~},
 * {@code *}) and (b) re-print method-call chains almost verbatim as Rust, since Rust supports the
 * same fluent {@code a.b().c()} syntax as Java. The compiler does not model what any given method
 * call "means" beyond that.
 */
public sealed interface Expr {

  /** A bare identifier reference, e.g. {@code userId}, or {@code nil} (which is just an id). */
  record VarUse(String name, boolean errableSuffix) implements Expr {}

  record StringLiteral(String javaText) implements Expr {}

  record IntLiteral(String text) implements Expr {}

  record BoolLiteral(boolean value) implements Expr {}

  /** An ordered array literal, e.g. {@code ["first", "second"]}. */
  record Array(List<Expr> elements) implements Expr {}

  record Not(Expr operand) implements Expr {}

  /** {@code left + right} or {@code left == right}. */
  record BinaryOp(Expr left, String operator, Expr right) implements Expr {}

  /** {@code expr :: ID} - a method reference. */
  record MethodRef(Expr target, String member) implements Expr {}

  /** {@code expr accessor ID} - a field/property access continuing a chain. */
  record MemberAccess(Expr target, Accessor accessor, String member) implements Expr {}

  /** {@code expr accessor func_chain} - a method-call chain continuing off a prior expression. */
  record ChainedCall(Expr target, Accessor accessor, FuncChain call) implements Expr {}

  /** A reference to a {@code #groupName} facet grouper used as a value. */
  record GrouperRef(String name) implements Expr {}

  /**
   * One call in a method chain: {@code name(args)} or {@code name { lambda-body }}. Covers both
   * {@code func_call} and {@code func_call_in_output_logic}.
   */
  record Call(
      String name, List<Expr> args, @Nullable LambdaBody lambda, boolean isNew, boolean isSpecial)
      implements Expr {}

  /** {@code (call accessor)* call} - a standalone chain of calls not hanging off a prior expr. */
  record FuncChain(List<Call> calls, List<Accessor> connectors) implements Expr {}

  /** The lambda/logic-block body passed to a {@link Call}, e.g. {@code {_.userId()}}. */
  record LambdaBody(
      boolean soon, boolean later, List<Statement> statements, @Nullable YieldStatement yield) {}
}
