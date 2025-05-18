package com.flipkart.krystal.lattice.codegen;

import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import javax.lang.model.element.TypeElement;

public record LatticeCodegenContext(
    CodegenPhase codegenPhase,
    TypeElement latticeAppTypeElement,
    VajramCodeGenUtility vajramCodeGenUtility) {}
