package com.flipkart.krystal.intellij.completion;

import com.flipkart.krystal.intellij.index.FacetInfo;
import com.flipkart.krystal.intellij.index.FacetKind;
import com.flipkart.krystal.intellij.index.VajramFacetIndex;
import com.flipkart.krystal.intellij.index.VajramInfo;
import com.flipkart.krystal.intellij.psi.VajramContextDetector;
import com.flipkart.krystal.intellij.psi.VajramPsiUtil;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public final class VajramCompletionContributor extends CompletionContributor {

  public VajramCompletionContributor() {
    extend(
        CompletionType.BASIC,
        PlatformPatterns.psiElement(),
        new CompletionProvider<>() {
          @Override
          protected void addCompletions(
              @NotNull CompletionParameters parameters,
              @NotNull ProcessingContext context,
              @NotNull CompletionResultSet result) {
            PsiElement position = parameters.getPosition();
            VajramContextDetector.ContextKind kind = VajramContextDetector.detect(position);
            if (kind == VajramContextDetector.ContextKind.NONE) {
              return;
            }
            PsiClass vajramClass = VajramPsiUtil.findEnclosingVajram(position);
            if (vajramClass == null) {
              return;
            }
            var project = position.getProject();
            GlobalSearchScope scope = GlobalSearchScope.allScope(project);
            VajramFacetIndex index = VajramFacetIndex.getInstance(project);
            VajramInfo vajramInfo = VajramPsiUtil.toVajramInfo(vajramClass);

            switch (kind) {
              case OUTPUT_METHOD_PARAMETERS -> suggestOutputFacets(index, vajramClass, result);
              case RESOLVE_DEP -> suggestDependencyFacetConstants(index, vajramInfo, scope, result);
              case RESOLVE_DEP_INPUTS ->
                  suggestDependencyInputConstants(index, vajramClass, scope, position, result);
              case DEPENDENCY_ON_VAJRAM -> suggestVajramClasses(index, scope, result);
              default -> {}
            }
          }
        });
  }

  private static void suggestOutputFacets(
      VajramFacetIndex index, PsiClass vajramClass, CompletionResultSet result) {
    for (FacetInfo facet : index.getOutputAvailableFacets(vajramClass)) {
      result.addElement(
          LookupElementBuilder.create(facet.name())
              .withTypeText(VajramPsiUtil.parameterTypeText(facet), true)
              .withTailText(" " + facet.kind(), true));
    }
  }

  private static void suggestDependencyFacetConstants(
      VajramFacetIndex index, VajramInfo vajramInfo, GlobalSearchScope scope, CompletionResultSet result) {
    for (String constant : index.getGeneratedFacetNameConstants(vajramInfo, scope)) {
      result.addElement(
          LookupElementBuilder.create(constant)
              .withTypeText(vajramInfo.facetsClassName(), true));
    }
    for (FacetInfo facet : index.getDependencyFacets(vajramInfo.vajramClass())) {
      result.addElement(
          LookupElementBuilder.create(facet.facetNameConstant())
              .withTypeText("predicted", true)
              .withTailText(" " + facet.qualifiedName(), true));
    }
  }

  private static void suggestDependencyInputConstants(
      VajramFacetIndex index,
      PsiClass vajramClass,
      GlobalSearchScope scope,
      PsiElement position,
      CompletionResultSet result) {
    String depFacetName = findSelectedDependencyFacetName(position);
    if (depFacetName == null) {
      return;
    }
    index
        .findDependencyFacet(vajramClass, depFacetName)
        .ifPresent(
            depFacet -> {
              if (depFacet.dependencyVajramName() == null) {
                return;
              }
              for (String constant :
                  index.getGeneratedRequestInputConstants(
                      depFacet.dependencyVajramName(), vajramClass, scope)) {
                result.addElement(
                    LookupElementBuilder.create(constant)
                        .withTypeText(depFacet.dependencyVajramName() + "_Req", true));
              }
              for (FacetInfo input :
                  index.getDependencyInputFacets(depFacet, scope)) {
                result.addElement(
                    LookupElementBuilder.create(input.facetNameConstant())
                        .withTypeText("predicted", true)
                        .withTailText(" " + input.qualifiedName(), true));
              }
            });
  }

  private static @org.jetbrains.annotations.Nullable String findSelectedDependencyFacetName(
      PsiElement position) {
    PsiClass vajramClass = VajramPsiUtil.findEnclosingVajram(position);
    if (vajramClass == null) {
      return null;
    }
    for (FacetInfo facet : VajramPsiUtil.collectFacets(vajramClass)) {
      if (facet.kind() == FacetKind.DEPENDENCY) {
        return facet.name();
      }
    }
    return null;
  }

  private static void suggestVajramClasses(
      VajramFacetIndex index, GlobalSearchScope scope, CompletionResultSet result) {
    for (PsiClass vajram : index.getAllVajrams(scope)) {
      if (vajram.getName() != null) {
        result.addElement(
            LookupElementBuilder.create(vajram.getName())
                .withTypeText(vajram.getQualifiedName(), true));
      }
    }
  }
}
