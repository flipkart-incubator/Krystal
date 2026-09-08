package com.flipkart.krystal.vajram.lang.rust.ast;

import java.nio.file.Path;
import java.util.List;

/** One Vajram declaration and its file-level package/import context. */
public record VajramFile(
    Path sourcePath, List<String> packageSegments, List<ImportDecl> imports, VajramDef vajram) {

  /** Grammar rule {@code imports_decl}: local Vajram name and the module it comes from. */
  public record ImportDecl(String vajramName, List<String> sourceSegments, boolean wildcard) {}
}
