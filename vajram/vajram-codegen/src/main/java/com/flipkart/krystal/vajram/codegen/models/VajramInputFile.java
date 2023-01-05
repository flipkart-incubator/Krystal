package com.flipkart.krystal.vajram.codegen.models;

import java.nio.file.Path;

public record VajramInputFile(String vajramName, Path srcRelativeFilePath, VajramInputsDef vajramInputsDef) {}
