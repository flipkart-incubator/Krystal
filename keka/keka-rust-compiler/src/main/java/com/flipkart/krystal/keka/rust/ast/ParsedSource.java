package com.flipkart.krystal.keka.rust.ast;

import java.util.List;

/** Everything declared in one {@code .keka} source file. */
public record ParsedSource(
    List<VajramFile> vajrams, List<AnnotationDefFile> annotationDefs, List<ModelFile> models) {

  public static final ParsedSource EMPTY = new ParsedSource(List.of(), List.of(), List.of());
}
