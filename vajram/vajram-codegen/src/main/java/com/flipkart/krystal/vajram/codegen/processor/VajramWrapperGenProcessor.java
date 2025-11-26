package com.flipkart.krystal.vajram.codegen.processor;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.FINAL;
import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.flipkart.krystal.codegen.common.models.Constants.MODULE_ROOT_PATH_KEY;

import com.flipkart.krystal.codegen.common.models.RunOnlyWhenCodegenPhaseIs;
import com.google.auto.service.AutoService;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@SupportedAnnotationTypes({
  "com.flipkart.krystal.vajram.Vajram",
  "com.flipkart.krystal.vajram.Trait"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions({CODEGEN_PHASE_KEY, MODULE_ROOT_PATH_KEY})
@RunOnlyWhenCodegenPhaseIs(FINAL)
public final class VajramWrapperGenProcessor extends AbstractVajramCodegenProcessor {}
