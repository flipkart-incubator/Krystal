package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.Utils.getInputUtilClassName;
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
public class VajramModelGenProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Utils util = new Utils(processingEnv, this.getClass());
    List<TypeElement> vajramDefinitions = util.getVajramClasses(roundEnv);
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
    }
    return true;
  }
}
