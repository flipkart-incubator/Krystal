package com.flipkart.krystal.codegen.common.spi;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import javax.lang.model.element.TypeElement;

/**
 * @param modelRootType for which to generate model subtypes
 * @param codegenPhase the current executing codegen phase
 * @param util {@link CodeGenUtility}
 */
public record ModelsCodeGenContext(
    TypeElement modelRootType, CodegenPhase codegenPhase, CodeGenUtility util) {}
