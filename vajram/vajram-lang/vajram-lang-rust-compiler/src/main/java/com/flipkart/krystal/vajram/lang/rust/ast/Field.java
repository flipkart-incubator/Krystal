package com.flipkart.krystal.vajram.lang.rust.ast;

import java.util.List;

/** A computed field declared with {@code type name = expr}. */
public record Field(
    List<String> annotations,
    TypeRef type,
    boolean fanout,
    String name,
    Expr value,
    SourceLocation location)
    implements ComputedFacet {}
