package com.flipkart.krystal.lattice.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.AbstractKrystalAnnoProcessor;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("com.flipkart.krystal.lattice.core.LatticeApp")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions(CODEGEN_PHASE_KEY)
public class LatticeAnnotationProcessor extends AbstractKrystalAnnoProcessor {

  @Override
  protected final boolean processImpl(
      Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    VajramCodeGenUtility util = new VajramCodeGenUtility(codeGenUtil());
    List<TypeElement> latticeApps =
        roundEnv.getElementsAnnotatedWith(LatticeApp.class).stream()
            .filter(element -> element.getKind() == ElementKind.CLASS)
            .map(executableElement -> (TypeElement) executableElement)
            .toList();

    for (TypeElement latticeAppTypeElement : latticeApps) {
      CharSequence message =
          "Lattice detected by %s: %s"
              .formatted(getClass().getSimpleName(), latticeAppTypeElement.getQualifiedName());
      util.codegenUtil().note(message);

      LatticeCodegenContext codegenContext =
          new LatticeCodegenContext(
              latticeAppTypeElement,
              requireNonNull(latticeAppTypeElement.getAnnotation(LatticeApp.class)),
              codegenPhase(),
              util,
              roundEnv);
      for (LatticeCodeGeneratorProvider customCodeGeneratorProvider :
          ServiceLoader.load(
              LatticeCodeGeneratorProvider.class, this.getClass().getClassLoader())) {
        try {
          customCodeGeneratorProvider.create(codegenContext).generate();
        } catch (Exception e) {
          util.codegenUtil().error(getStackTraceAsString(e), latticeAppTypeElement);
        }
      }
    }

    return false;
  }
}
