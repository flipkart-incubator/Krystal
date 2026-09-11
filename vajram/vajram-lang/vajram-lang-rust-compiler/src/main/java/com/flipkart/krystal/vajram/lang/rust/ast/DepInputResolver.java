package com.flipkart.krystal.vajram.lang.rust.ast;

import java.util.List;

/**
 * How one (or more, comma-grouped) of a dependency's inputs is resolved: either a plain expression
 * list ({@code dep_input_resolver_stat}) or a computed logic block ({@code
 * dep_input_resolver_func}). {@code fanout} marks the {@code =*} form which produces one dependency
 * invocation per element instead of a single one.
 */
public sealed interface DepInputResolver {

  List<String> targetInputs();

  boolean fanout();

  record Stat(List<String> targetInputs, boolean fanout, List<Expr> values)
      implements DepInputResolver {}

  record Func(List<String> targetInputs, boolean fanout, LogicBlock body)
      implements DepInputResolver {}
}
