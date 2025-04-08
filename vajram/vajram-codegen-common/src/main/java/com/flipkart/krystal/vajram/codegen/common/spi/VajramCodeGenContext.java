package com.flipkart.krystal.vajram.codegen.common.spi;

import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;

public record VajramCodeGenContext(VajramInfo vajramInfo, Utils util, CodegenPhase codegenPhase) {}
