package com.flipkart.krystal.vajram.codegen.processor;

import static com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase.SCHEMAS;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.CODEGEN_PHASE_KEY;

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
@SupportedOptions(CODEGEN_PHASE_KEY)
public final class VajramSchemaGenProcessor extends AbstractVajramCodegenProcessor {

  public VajramSchemaGenProcessor() {
    super(MODELS);
  }
}
