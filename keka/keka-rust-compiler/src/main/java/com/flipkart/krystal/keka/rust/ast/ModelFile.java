package com.flipkart.krystal.keka.rust.ast;

import java.nio.file.Path;
import java.util.List;

/** One model and its file-level package/import context. */
public record ModelFile(
    Path sourcePath,
    List<String> packageSegments,
    List<VajramAnnotation> packageAnnotations,
    List<VajramFile.ImportDecl> imports,
    ModelDef model) {}
