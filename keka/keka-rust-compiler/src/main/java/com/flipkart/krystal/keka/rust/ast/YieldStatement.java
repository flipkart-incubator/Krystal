package com.flipkart.krystal.keka.rust.ast;

import java.util.List;

/** Grammar rule {@code yield_statement}: the trailing expression(s) of a logic block. */
public record YieldStatement(List<Expr> values) {}
