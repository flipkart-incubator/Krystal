package com.flipkart.krystal.lattice.codegen;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class LatticeCodegenUtils {
  public static final String LATTICE_APP_IMPL_SUFFIX = "_Impl";
  public static final String APP_DOPANT_NAME_SEPARATOR = "_";
  public static final String DOPANT_IMPL_SUFFIX = "_Impl";

  private final CodeGenUtility util;

  public LatticeCodegenUtils(CodeGenUtility util) {
    this.util = util;
  }

  public ClassName getDopantImplName(
      TypeElement latticeAppElem, Class<? extends Dopant<?, ?>> dopantClass) {
    String packageName =
        util.processingEnv()
            .getElementUtils()
            .getPackageOf(latticeAppElem)
            .getQualifiedName()
            .toString();

    return ClassName.get(
        packageName,
        latticeAppElem.getSimpleName().toString()
            + APP_DOPANT_NAME_SEPARATOR
            + dopantClass.getSimpleName()
            + DOPANT_IMPL_SUFFIX);
  }

  public MethodSpec.Builder constructorOverride(Class<?> clazz) {
    TypeElement clazzElement =
        requireNonNull(
            util.processingEnv()
                .getElementUtils()
                .getTypeElement(requireNonNull(clazz.getCanonicalName())));

    List<ExecutableElement> injectionCtors = new ArrayList<>();
    ExecutableElement noArgCtor = null;
    for (Element element : clazzElement.getEnclosedElements()) {
      if (!isVisible(element)) {
        continue;
      }
      if (element.getKind() == ElementKind.CONSTRUCTOR) {
        ExecutableElement c = (ExecutableElement) element;
        if (c.getAnnotation(Inject.class) != null) {
          injectionCtors.add(c);
        }
        if (c.getParameters().isEmpty()) {
          noArgCtor = c;
        }
      }
    }
    ExecutableElement parentCtor;
    if (injectionCtors.size() > 1 || (injectionCtors.isEmpty() && noArgCtor == null)) {
      throw util.errorAndThrow(
          "A class which needs to be overriden must have exactly one public/protected constructor with the @Inject annotation OR a public/protected no arg constructor. Please fix :"
              + clazz,
          clazzElement);
    }
    if (!injectionCtors.isEmpty()) {
      parentCtor = injectionCtors.get(0);
    } else {
      parentCtor = requireNonNull(noArgCtor, "Cannot be null here because of the checks above");
    }
    var constructorBuilder = MethodSpec.constructorBuilder().addAnnotation(Inject.class);

    List<CodeBlock> params = new ArrayList<>();
    for (VariableElement parameter : parentCtor.getParameters()) {
      constructorBuilder.addParameter(
          TypeName.get(parameter.asType()), parameter.getSimpleName().toString());
      params.add(CodeBlock.of("$L", parameter.getSimpleName().toString()));
    }
    return constructorBuilder.addStatement(
        "super($L)", params.stream().collect(CodeBlock.joining(",")));
  }

  public static ClassName getDiBindingContainerName(
      LatticeCodegenContext context, String identifier) {
    return ClassName.get(
        context.codeGenUtility().codegenUtil().getPackageName(context.latticeAppTypeElement()),
        Stream.of(context.latticeAppTypeElement().getSimpleName().toString(), identifier, "Module")
            .filter(s -> !s.isBlank())
            .collect(Collectors.joining("_")));
  }

  private static boolean isVisible(Element element) {
    return element.getModifiers().contains(Modifier.PUBLIC)
        || element.getModifiers().contains(Modifier.PROTECTED);
  }
}
