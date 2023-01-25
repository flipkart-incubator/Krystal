package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.vajram.codegen.InputFilePath;

public record VajramInputFile(String vajramName, InputFilePath inputFilePath, VajramInputsDef vajramInputsDef) {}
