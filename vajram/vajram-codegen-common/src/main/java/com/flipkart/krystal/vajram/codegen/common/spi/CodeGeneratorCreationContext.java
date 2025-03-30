package com.flipkart.krystal.vajram.codegen.common.spi;

import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;

public record CodeGeneratorCreationContext(
    VajramInfo vajramInfo, Utils util, CodegenPhase codegenPhase) {}
