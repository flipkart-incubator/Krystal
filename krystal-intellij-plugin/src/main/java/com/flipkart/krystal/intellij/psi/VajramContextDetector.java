package com.flipkart.krystal.intellij.psi;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INPUTS_CLASS;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INTERNAL_FACETS_CLASS;

import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PsiTreeUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class VajramContextDetector {

  public enum ContextKind {
    NONE,
    OUTPUT_METHOD_PARAMETERS,
    RESOLVE_DEP,
    RESOLVE_DEP_INPUTS,
    DEPENDENCY_ON_VAJRAM,
    INPUTS_MEMBER,
    INTERNAL_FACETS_MEMBER
  }

  private VajramContextDetector() {}

  public static ContextKind detect(PsiElement element) {
    PsiClass vajramClass = VajramPsiUtil.findEnclosingVajram(element);
    if (vajramClass == null) {
      return ContextKind.NONE;
    }
    PsiMethod outputMethod = VajramPsiUtil.findOutputMethod(vajramClass);
    if (outputMethod != null
        && PsiTreeUtil.isAncestor(outputMethod.getParameterList(), element, false)) {
      return ContextKind.OUTPUT_METHOD_PARAMETERS;
    }
    PsiMethod resolveMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
    if (resolveMethod != null && VajramPsiUtil.hasAnnotation(resolveMethod, Resolve.class)) {
      PsiAnnotation resolve = resolveMethod.getAnnotation(Resolve.class.getName());
      if (resolve != null && isInsideAnnotationAttribute(resolve, element, "dep")) {
        return ContextKind.RESOLVE_DEP;
      }
      if (resolve != null && isInsideAnnotationAttribute(resolve, element, "depInputs")) {
        return ContextKind.RESOLVE_DEP_INPUTS;
      }
    }
    PsiElement parent = element.getParent();
    if (parent instanceof PsiJavaCodeReferenceElement ref
        && ref.getParent() instanceof PsiNameValuePair pair
        && "onVajram".equals(pair.getName())) {
      PsiAnnotation dependency = PsiTreeUtil.getParentOfType(pair, PsiAnnotation.class);
      if (dependency != null && Dependency.class.getName().equals(dependency.getQualifiedName())) {
        return ContextKind.DEPENDENCY_ON_VAJRAM;
      }
    }
    PsiClass nested = PsiTreeUtil.getParentOfType(element, PsiClass.class);
    if (nested != null) {
      if (_INPUTS_CLASS.equals(nested.getName())) {
        return ContextKind.INPUTS_MEMBER;
      }
      if (_INTERNAL_FACETS_CLASS.equals(nested.getName())) {
        return ContextKind.INTERNAL_FACETS_MEMBER;
      }
    }
    return ContextKind.NONE;
  }

  private static boolean isInsideAnnotationAttribute(
      PsiAnnotation annotation, PsiElement element, String attributeName) {
    for (PsiNameValuePair pair : annotation.getParameterList().getAttributes()) {
      if (attributeName.equals(pair.getName()) && PsiTreeUtil.isAncestor(pair, element, false)) {
        return true;
      }
      if (pair.getName() == null
          && "dep".equals(attributeName)
          && PsiTreeUtil.isAncestor(pair, element, false)) {
        return true;
      }
    }
    return false;
  }

  public static @Nullable FacetInfoAtCaret facetAtOutputParameter(PsiElement element) {
    PsiParameter parameter = PsiTreeUtil.getParentOfType(element, PsiParameter.class);
    if (parameter == null) {
      return null;
    }
    return new FacetInfoAtCaret(parameter.getName(), parameter.getType().getPresentableText());
  }

  public record FacetInfoAtCaret(String name, String typeText) {}
}
