package com.flipkart.krystal.vajram.codegen.common.spi;

import com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import java.util.List;

public record AllVajramCodeGenContext(
    List<VajramInfo> vajramInfos, CodeGenUtility util, CodegenPhase codegenPhase) {}
