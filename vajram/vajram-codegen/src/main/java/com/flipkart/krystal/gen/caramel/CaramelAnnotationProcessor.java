package com.flipkart.krystal.gen.caramel;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.util.stream.Collectors.toSet;

import com.flipkart.krystal.caramel.model.ImplAs;
import com.google.auto.service.AutoService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("com.flipkart.krystal.caramel.model.CaramelPayload")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class CaramelAnnotationProcessor extends AbstractProcessor {

  public static final String DEFINITION_SUFFIX = "Definition";

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      Set<TypeElement> payloadDefinitions =
          roundEnv.getElementsAnnotatedWith(annotation).stream()
              .filter(element -> element.getKind() == ElementKind.INTERFACE)
              .map(executableElement -> (TypeElement) executableElement)
              .collect(toSet());
      if (payloadDefinitions.isEmpty()) {
        continue;
      }
      for (TypeElement payloadDefinition : payloadDefinitions) {
        Optional<String> payloadDefinitionName =
            getPayloadClassName(payloadDefinition, processingEnv);
        if (payloadDefinitionName.isEmpty()) {
          continue;
        }
        if (payloadDefinition.getNestingKind().isNested()) {
          processingEnv
              .getMessager()
              .printMessage(
                  Kind.ERROR,
                  "Payload definition interface should not be nested",
                  payloadDefinition);
        }
        CaramelCodeGenerator caramelCodeGenerator =
            new CaramelCodeGenerator(payloadDefinition, payloadDefinitionName.get(), processingEnv);
        JavaFileObject builderFile;
        try {
          builderFile =
              processingEnv
                  .getFiler()
                  .createSourceFile(
                      caramelCodeGenerator.getPayloadImplPackageName()
                          + '.'
                          + payloadDefinitionName.get(),
                      payloadDefinition);
        } catch (IOException e) {
          processingEnv
              .getMessager()
              .printMessage(
                  Kind.ERROR,
                  "Could not create java source file for payload implementation"
                      + getStackTraceAsString(e),
                  payloadDefinition);
          continue;
        }
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
          out.println(caramelCodeGenerator.generatePayloadImpl());
        } catch (IOException e) {
          processingEnv
              .getMessager()
              .printMessage(Kind.ERROR, "Error opening writer", payloadDefinition);
        }
      }
    }
    return true;
  }

  private static Optional<String> getPayloadClassName(
      TypeElement payloadDefinitionIface, ProcessingEnvironment processingEnv) {
    String payloadDefinitionName = payloadDefinitionIface.getQualifiedName().toString();
    Optional<String> payloadClassName;
    Optional<ImplAs> implAs =
        Optional.ofNullable(payloadDefinitionIface.getAnnotation(ImplAs.class));
    if (implAs.isPresent()) {
      payloadClassName = Optional.of(implAs.get().value());
    } else if (!payloadDefinitionName.endsWith(DEFINITION_SUFFIX)) {
      processingEnv
          .getMessager()
          .printMessage(
              Kind.ERROR,
              ("Caramel Payload definition class must either be "
                      + "annotedWith '%s' or must have a name ending in 'Definition'")
                  .formatted(ImplAs.class),
              payloadDefinitionIface);
      payloadClassName = Optional.empty();
    } else {
      payloadClassName =
          Optional.of(
              payloadDefinitionName.substring(
                  0, payloadDefinitionName.length() - DEFINITION_SUFFIX.length()));
    }
    return payloadClassName;
  }
}
