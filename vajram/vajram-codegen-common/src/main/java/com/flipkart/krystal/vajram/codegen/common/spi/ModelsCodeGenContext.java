package com.flipkart.krystal.vajram.codegen.common.spi;

import com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import javax.lang.model.element.TypeElement;

public record ModelsCodeGenContext(
    TypeElement modelRootType, CodeGenUtility util, CodegenPhase codegenPhase) {}
