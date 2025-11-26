package com.flipkart.krystal.vajram.codegen.common.spi;

import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;

public record VajramCodeGenContext(
    VajramInfo vajramInfo, VajramCodeGenUtility util, CodegenPhase codegenPhase) {}
