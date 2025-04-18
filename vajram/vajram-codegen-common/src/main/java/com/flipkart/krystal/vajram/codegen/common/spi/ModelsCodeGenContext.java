package com.flipkart.krystal.vajram.codegen.common.spi;

import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import javax.lang.model.element.TypeElement;

public record ModelsCodeGenContext(
    TypeElement modelRootType, Utils util, CodegenPhase codegenPhase) {}
