package com.flipkart.krystal.vajram.codegen.common.spi;

import com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;

public record VajramCodeGenContext(
    VajramInfo vajramInfo, CodeGenUtility util, CodegenPhase codegenPhase) {}
