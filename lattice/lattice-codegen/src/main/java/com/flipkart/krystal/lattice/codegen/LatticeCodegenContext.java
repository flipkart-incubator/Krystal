package com.flipkart.krystal.lattice.codegen;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import javax.lang.model.element.TypeElement;

public record LatticeCodegenContext(
    TypeElement latticeAppTypeElement,
    LatticeApp latticeApp,
    CodegenPhase codegenPhase,
    VajramCodeGenUtility codeGenUtility,
    javax.annotation.processing.RoundEnvironment roundEnv) {}
