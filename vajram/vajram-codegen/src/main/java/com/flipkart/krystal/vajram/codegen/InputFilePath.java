package com.flipkart.krystal.vajram.codegen;

import java.nio.file.Path;

record InputFilePath(Path srcDir, Path relativeFilePath) {

  public Path absolutePath() {
    return srcDir.resolve(relativeFilePath);
  }
}
