package com.flipkart.krystal.keka.rust.ast;

import java.nio.file.Path;
import java.util.List;

/** One annotation definition and its file-level package/import context. */
public record AnnotationDefFile(
    Path sourcePath,
    List<String> packageSegments,
    List<VajramAnnotation> packageAnnotations,
    List<VajramFile.ImportDecl> imports,
    AnnotationDef definition) {}
