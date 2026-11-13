package com.flipkart.krystal.keka.rust.ast;

import java.util.List;

/** A computed field declared with {@code type name = expr}. */
public record Field(
    List<VajramAnnotation> annotations,
    TypeRef type,
    boolean fanout,
    String name,
    Expr value,
    SourceLocation location)
    implements ComputedFacet {}
