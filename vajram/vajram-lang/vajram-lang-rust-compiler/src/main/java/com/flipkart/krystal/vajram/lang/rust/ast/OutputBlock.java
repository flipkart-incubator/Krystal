package com.flipkart.krystal.vajram.lang.rust.ast;

import java.util.List;

/**
 * Grammar rule {@code output_block}: either an inline logic block or a delegating dependency call.
 */
public sealed interface OutputBlock {

  record Logic(List<String> annotations, boolean soon, boolean later, LogicBlock body)
      implements OutputBlock {}

  record Delegate(DependencyInvocation invocation) implements OutputBlock {}
}
