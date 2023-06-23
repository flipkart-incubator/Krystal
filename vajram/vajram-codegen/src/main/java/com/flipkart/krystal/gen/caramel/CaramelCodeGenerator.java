package com.flipkart.krystal.gen.caramel;

import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.stream.Collectors.toSet;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec.Builder;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

public class CaramelCodeGenerator {
  private final TypeElement payloadDefinitionIface;
  private final ProcessingEnvironment processingEnv;
  private final String payloadImplClassName;
  private final PackageElement payloadPackage;

  public CaramelCodeGenerator(
      TypeElement payloadDefinitionIface,
      String payloadImplClassName,
      ProcessingEnvironment processingEnv) {
    if (payloadDefinitionIface.getNestingKind().isNested()) {
      throw new IllegalArgumentException("Payload definition class cannot be nested");
    }
    this.payloadDefinitionIface = payloadDefinitionIface;
    this.processingEnv = processingEnv;
    this.payloadImplClassName = payloadImplClassName;
    this.payloadPackage = (PackageElement) payloadDefinitionIface.getEnclosingElement();
  }

  public Optional<String> getPayloadImplClassName() {
    return Optional.ofNullable(payloadImplClassName);
  }

  public String getPayloadImplPackageName() {
    return payloadPackage.getQualifiedName().toString();
  }

  public String generatePayloadImpl() throws IOException {
    Set<ExecutableElement> executableElements =
        ElementFilter.methodsIn(Set.of(payloadDefinitionIface)).stream()
            .filter(executableElement -> executableElement.getParameters().isEmpty())
            .collect(toSet());

    StringWriter writer = new StringWriter();
    Builder payloadImplClass = classBuilder(payloadImplClassName).addModifiers(Modifier.FINAL);

    JavaFile.builder(payloadPackage.getQualifiedName().toString(), payloadImplClass.build())
        .build()
        .writeTo(writer);
    return writer.toString();
  }
}
