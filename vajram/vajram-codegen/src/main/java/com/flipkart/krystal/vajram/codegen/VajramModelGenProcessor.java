package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getInputUtilClassName;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getVajramImplClassName;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.codegen.models.VajramInfo;
import com.google.auto.service.AutoService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes("com.flipkart.krystal.vajram.VajramDef")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class VajramModelGenProcessor extends AbstractProcessor {

  public static final String BUILD_PHASE_FILE_NAME = "krystal_build_phase.txt";

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    AnnotationProcessingUtils util = new AnnotationProcessingUtils(roundEnv, processingEnv);
    BuildPhase buildPhase;
    try {
      buildPhase = readBuildPhase();
    } catch (IOException e) {
      util.note("Did not find %s".formatted(BUILD_PHASE_FILE_NAME));
      return false;
    }
    if (buildPhase == null) {
      util.note("Build phase in null");
      return false;
    }
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

      if (BuildPhase.CODEGEN_MODELS.equals(buildPhase)) {
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
      } else if (BuildPhase.CODEGEN_IMPLS.equals(buildPhase)) {
        util.generateSourceFile(
            vajramCodeGenerator.getPackageName()
                + '.'
                + getVajramImplClassName(vajramInfo.vajramId().vajramId()),
            vajramCodeGenerator.codeGenVajramImpl(),
            vajramClass);
      } else {
        util.log(Kind.NOTE, "Unknown build phase %s".formatted(buildPhase), vajramClass);
      }
    }
    return true;
  }

  private BuildPhase readBuildPhase() throws IOException {
    return BuildPhase.valueOf(
        Files.readString(
            new File(
                    processingEnv
                        .getFiler()
                        .getResource(StandardLocation.SOURCE_OUTPUT, "", BUILD_PHASE_FILE_NAME)
                        .toUri())
                .toPath()));
  }
}
