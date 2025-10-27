package com.flipkart.krystal.vajram.codegen.processor;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;

import com.flipkart.krystal.codegen.common.models.RunOnlyWhenCodegenPhaseIs;
import com.google.auto.service.AutoService;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@SupportedAnnotationTypes({
  "com.flipkart.krystal.vajram.Vajram",
  "com.flipkart.krystal.vajram.Trait",
  "com.flipkart.krystal.annos.Generated"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions(CODEGEN_PHASE_KEY)
@RunOnlyWhenCodegenPhaseIs(MODELS)
public final class VajramModelGenProcessor extends AbstractVajramCodegenProcessor {}
