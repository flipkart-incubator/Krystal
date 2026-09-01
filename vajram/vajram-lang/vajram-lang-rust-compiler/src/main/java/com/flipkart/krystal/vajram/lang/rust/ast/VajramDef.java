package com.flipkart.krystal.vajram.lang.rust.ast;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Grammar rule {@code vajram_def}, minus the package/import decls (see {@link VajramFile}). */
public record VajramDef(
    String name,
    List<VajramAnnotation> annotations,
    List<InputDecl> inputs,
    TypeRef outputType,
    @Nullable Callers callers,
    List<InjectionDecl> injections,
    List<ComputedFacet> computedFacets,
    OutputBlock outputBlock,
    SourceLocation location) {}
