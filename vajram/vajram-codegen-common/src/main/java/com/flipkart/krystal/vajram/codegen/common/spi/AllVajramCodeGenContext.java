package com.flipkart.krystal.vajram.codegen.common.spi;

import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import java.util.List;

public record AllVajramCodeGenContext(
    List<VajramInfo> vajramInfos, VajramCodeGenUtility util, CodegenPhase codegenPhase) {}
