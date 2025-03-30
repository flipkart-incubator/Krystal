package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase.WRAPPERS;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.CODEGEN_PHASE_KEY;

import com.google.auto.service.AutoService;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@SupportedAnnotationTypes({
  "com.flipkart.krystal.vajram.Vajram",
  "com.flipkart.krystal.vajram.VajramTrait"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions(CODEGEN_PHASE_KEY)
public final class VajramWrapperGenProcessor extends AbstractVajramCodegenProcessor {

  public VajramWrapperGenProcessor() {
    super(WRAPPERS);
  }
}
