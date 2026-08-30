package com.flipkart.krystal.vajram.lang.rust.ast;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Grammar rule {@code logic_block}: {@code '{' statement* yield_statement? '}'}. */
public record LogicBlock(List<Statement> statements, @Nullable YieldStatement yield) {}
