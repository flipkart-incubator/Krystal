package com.flipkart.krystal.intellij.psi;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACET_NAME_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.QUALIFIED_FACET_SEPARATOR;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.REQUEST_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INPUTS_CLASS;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INTERNAL_FACETS_CLASS;

import com.flipkart.krystal.intellij.index.FacetInfo;
import com.flipkart.krystal.intellij.index.FacetKind;
import com.flipkart.krystal.intellij.index.VajramInfo;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import jakarta.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class VajramPsiUtil {

  private VajramPsiUtil() {}

  public static boolean isVajramOrTrait(PsiClass psiClass) {
    return hasAnnotation(psiClass, Vajram.class) || hasAnnotation(psiClass, Trait.class);
  }

  public static boolean isVajram(PsiClass psiClass) {
    return hasAnnotation(psiClass, Vajram.class);
  }

  public static boolean isTrait(PsiClass psiClass) {
    return hasAnnotation(psiClass, Trait.class);
  }

  public static @Nullable PsiClass findEnclosingVajram(PsiElement element) {
    PsiClass enclosing = PsiTreeUtil.getParentOfType(element, PsiClass.class);
    while (enclosing != null) {
      if (isVajramOrTrait(enclosing)) {
        return enclosing;
      }
      enclosing = PsiTreeUtil.getParentOfType(enclosing, PsiClass.class);
    }
    return null;
  }

  public static @Nullable PsiClass findNestedClass(PsiClass vajramClass, String nestedName) {
    for (PsiClass inner : vajramClass.getInnerClasses()) {
      if (nestedName.equals(inner.getName())) {
        return inner;
      }
    }
    return null;
  }

  public static VajramInfo toVajramInfo(PsiClass vajramClass) {
    return new VajramInfo(vajramClass, vajramClass.getName(), isTrait(vajramClass));
  }

  public static List<FacetInfo> collectFacets(PsiClass vajramClass) {
    List<FacetInfo> facets = new ArrayList<>();
    PsiClass inputs = findNestedClass(vajramClass, _INPUTS_CLASS);
    if (inputs != null) {
      facets.addAll(collectContainerFacets(inputs, FacetKind.INPUT, vajramClass.getName()));
    }
    PsiClass internalFacets = findNestedClass(vajramClass, _INTERNAL_FACETS_CLASS);
    if (internalFacets != null) {
      facets.addAll(collectInternalFacets(internalFacets, vajramClass.getName()));
    }
    return facets;
  }

  private static List<FacetInfo> collectContainerFacets(
      PsiClass container, FacetKind kind, String vajramId) {
    List<FacetInfo> facets = new ArrayList<>();
    for (PsiField field : container.getFields()) {
      if (field.getName() != null && field.getName().startsWith("_")) {
        continue;
      }
      facets.add(toFacetInfo(field.getName(), kind, field.getType(), field, vajramId, null));
    }
    for (PsiMethod method : container.getMethods()) {
      if (!method.hasModifierProperty(PsiModifier.ABSTRACT)
          || method.getParameterList().getParametersCount() != 0
          || method.getReturnType() == null
          || PsiType.VOID.equals(method.getReturnType())) {
        continue;
      }
      String name = method.getName();
      if (name.startsWith("_")) {
        continue;
      }
      facets.add(toFacetInfo(name, kind, method.getReturnType(), method, vajramId, null));
    }
    return facets;
  }

  private static List<FacetInfo> collectInternalFacets(PsiClass container, String vajramId) {
    List<FacetInfo> facets = new ArrayList<>();
    for (PsiField field : container.getFields()) {
      if (field.getName() == null || field.getName().startsWith("_")) {
        continue;
      }
      FacetKind kind =
          hasAnnotation(field, Dependency.class)
              ? FacetKind.DEPENDENCY
              : hasAnnotation(field, Inject.class) ? FacetKind.INJECTION : null;
      if (kind == null) {
        continue;
      }
      facets.add(
          toFacetInfo(
              field.getName(),
              kind,
              field.getType(),
              field,
              vajramId,
              resolveDependencyVajramName(field)));
    }
    for (PsiMethod method : container.getMethods()) {
      if (!method.hasModifierProperty(PsiModifier.ABSTRACT)
          || method.getParameterList().getParametersCount() != 0
          || method.getReturnType() == null
          || PsiType.VOID.equals(method.getReturnType())) {
        continue;
      }
      String name = method.getName();
      if (name.startsWith("_")) {
        continue;
      }
      FacetKind kind =
          hasAnnotation(method, Dependency.class)
              ? FacetKind.DEPENDENCY
              : hasAnnotation(method, Inject.class) ? FacetKind.INJECTION : null;
      if (kind == null) {
        continue;
      }
      facets.add(
          toFacetInfo(
              name,
              kind,
              method.getReturnType(),
              method,
              vajramId,
              resolveDependencyVajramName(method)));
    }
    return facets;
  }

  private static FacetInfo toFacetInfo(
      String name,
      FacetKind kind,
      PsiType type,
      PsiElement element,
      String vajramId,
      @Nullable String dependencyVajramName) {
    boolean mandatory = isMandatoryOnServer(element);
    boolean optionalForClient = isOptionalForClient(element);
    String qualifiedName = vajramId + QUALIFIED_FACET_SEPARATOR + name;
    return new FacetInfo(
        name, kind, type, mandatory, optionalForClient, dependencyVajramName, qualifiedName);
  }

  private static boolean isMandatoryOnServer(PsiElement element) {
    if (!(element instanceof PsiModifierListOwner owner)) {
      return false;
    }
    PsiAnnotation ifAbsent = owner.getAnnotation(IfAbsent.class.getName());
    if (ifAbsent == null) {
      return false;
    }
    String text = ifAbsent.getText();
    return text.contains("FAIL") || text.contains("ASSUME_DEFAULT_VALUE");
  }

  private static boolean isOptionalForClient(PsiElement element) {
    if (!(element instanceof PsiModifierListOwner owner)) {
      return true;
    }
    return owner.getAnnotation(IfAbsent.class.getName()) == null;
  }

  private static @Nullable String resolveDependencyVajramName(PsiElement element) {
    if (!(element instanceof PsiModifierListOwner owner)) {
      return null;
    }
    PsiAnnotation dependency = owner.getAnnotation(Dependency.class.getName());
    if (dependency == null) {
      return null;
    }
    PsiNameValuePair[] attributes = dependency.getParameterList().getAttributes();
    for (PsiNameValuePair attribute : attributes) {
      if ("onVajram".equals(attribute.getName())) {
        PsiJavaCodeReferenceElement value = (PsiJavaCodeReferenceElement) attribute.getValue();
        if (value != null) {
          return value.getReferenceName();
        }
      }
      if ("withVajramReq".equals(attribute.getName())) {
        PsiJavaCodeReferenceElement value = (PsiJavaCodeReferenceElement) attribute.getValue();
        if (value != null) {
          String reqName = value.getReferenceName();
          if (reqName != null && reqName.endsWith(REQUEST_SUFFIX)) {
            return reqName.substring(0, reqName.length() - REQUEST_SUFFIX.length());
          }
          return reqName;
        }
      }
    }
    return null;
  }

  public static List<PsiClass> findAllVajrams(Project project, GlobalSearchScope scope) {
    List<PsiClass> result = new ArrayList<>();
    JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
    for (Class<? extends Annotation> annotationType : List.of(Vajram.class, Trait.class)) {
      PsiClass annotationClass = facade.findClass(annotationType.getName(), scope);
      if (annotationClass == null) {
        continue;
      }
      AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope)
          .forEach(
              psiClass -> {
                if (isVajramOrTrait(psiClass)) {
                  result.add(psiClass);
                }
              });
    }
    return result;
  }

  public static Optional<PsiClass> findGeneratedClass(
      Project project, GlobalSearchScope scope, String className) {
    PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(className, scope);
    return Optional.ofNullable(psiClass);
  }

  public static List<String> collectGeneratedStringConstants(
      Project project, GlobalSearchScope scope, String generatedClassName) {
    List<String> constants = new ArrayList<>();
    PsiClass generatedClass =
        JavaPsiFacade.getInstance(project).findClass(generatedClassName, scope);
    if (generatedClass == null) {
      return constants;
    }
    for (PsiField field : generatedClass.getFields()) {
      if (field.hasModifierProperty(PsiModifier.STATIC)
          && field.hasModifierProperty(PsiModifier.FINAL)
          && field.getName() != null
          && field.getName().endsWith(FACET_NAME_SUFFIX)) {
        constants.add(field.getName());
      }
    }
    return constants;
  }

  public static @Nullable PsiMethod findOutputMethod(PsiClass vajramClass) {
    for (PsiMethod method : vajramClass.getMethods()) {
      if (hasAnnotation(method, Output.class)) {
        return method;
      }
    }
    return null;
  }

  public static boolean hasAnnotation(PsiElement element, Class<? extends Annotation> annotation) {
    return element instanceof PsiModifierListOwner owner
        && owner.getAnnotation(annotation.getName()) != null;
  }

  public static String parameterTypeText(FacetInfo facet) {
    if (facet.kind() == FacetKind.INJECTION && !facet.mandatoryOnServer()) {
      return "@" + shortName(Nullable.class.getName()) + " " + facet.type().getPresentableText();
    }
    if (facet.optionalForClient() && facet.kind() == FacetKind.INPUT) {
      return "java.util.Optional<" + facet.type().getPresentableText() + ">";
    }
    return facet.type().getPresentableText();
  }

  private static String shortName(String fqn) {
    int idx = fqn.lastIndexOf('.');
    return idx >= 0 ? fqn.substring(idx + 1) : fqn;
  }

  public static @Nullable PsiClassType resolveDependencyVajramClass(
      Project project, GlobalSearchScope scope, FacetInfo dependencyFacet) {
    if (dependencyFacet.dependencyVajramName() == null) {
      return null;
    }
    PsiClass depClass =
        JavaPsiFacade.getInstance(project).findClass(dependencyFacet.dependencyVajramName(), scope);
    if (depClass == null) {
      return null;
    }
    return JavaPsiFacade.getInstance(project).getElementFactory().createType(depClass);
  }
}
