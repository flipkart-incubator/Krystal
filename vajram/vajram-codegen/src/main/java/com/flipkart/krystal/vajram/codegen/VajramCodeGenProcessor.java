package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.CodegenUtils.getInputUtilClassName;
import static com.flipkart.krystal.vajram.codegen.CodegenUtils.getVajramImplClassName;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.codegen.models.VajramInfo;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("com.flipkart.krystal.vajram.VajramDef")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class VajramCodeGenProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    AnnotationProcessingUtils util = new AnnotationProcessingUtils(roundEnv, processingEnv);
    List<TypeElement> vajramDefinitions = util.getVajramClasses();
    util.note(
        "Vajram Defs received by VajramModelGenProcessor: %s"
            .formatted(
                vajramDefinitions.stream()
                    .map(Objects::toString)
                    .collect(
                        joining(lineSeparator(), '[' + lineSeparator(), lineSeparator() + ']'))));
    for (TypeElement vajramClass : vajramDefinitions) {
      VajramInfo vajramInfo = util.computeVajramInfo(vajramClass);

      VajramCodeGenerator vajramCodeGenerator = util.createCodeGenerator(vajramInfo);

      util.generateSourceFile(
          vajramCodeGenerator.getPackageName() + '.' + vajramCodeGenerator.getRequestClassName(),
          vajramCodeGenerator.codeGenVajramRequest(),
          vajramClass);
      util.generateSourceFile(
          vajramCodeGenerator.getPackageName()
              + '.'
              + getInputUtilClassName(vajramCodeGenerator.getVajramName()),
          vajramCodeGenerator.codeGenInputUtil(),
          vajramClass);
      try {
        util.generateSourceFile(
            vajramCodeGenerator.getPackageName()
                + '.'
                + getVajramImplClassName(vajramInfo.vajramId().vajramId()),
            vajramCodeGenerator.codeGenVajramImpl(),
            vajramClass);
      } catch (Exception e) {
        util.note(
            "Error while generating vajramImpl. Ignoring the error as it should be skipped in subsequent runs");
      }
    }
    return true;
  }
}
