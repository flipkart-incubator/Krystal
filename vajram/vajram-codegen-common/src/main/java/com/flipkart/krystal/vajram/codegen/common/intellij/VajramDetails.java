package com.flipkart.krystal.vajram.codegen.common.intellij;

import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import java.nio.file.Path;

record VajramDetails(VajramInfo vajramInfo, Path vajramFilePath, Path modulePath) {}
