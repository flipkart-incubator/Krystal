package com.flipkart.krystal.intellij.actions;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACETS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.REQUEST_SUFFIX;

import com.flipkart.krystal.intellij.index.FacetInfo;
import com.flipkart.krystal.intellij.index.FacetKind;
import com.flipkart.krystal.intellij.index.VajramFacetIndex;
import com.flipkart.krystal.intellij.psi.VajramPsiUtil;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;
import java.util.List;

public final class GenerateResolverAction extends VajramActionBase {

  @Override
  protected boolean isApplicable(PsiClass vajramClass) {
    return VajramPsiUtil.isVajram(vajramClass)
        && !VajramFacetIndex.getInstance(vajramClass.getProject())
            .getDependencyFacets(vajramClass)
            .isEmpty();
  }

  @Override
  protected void perform(Project project, PsiClass vajramClass, AnActionEvent e) {
    VajramFacetIndex index = VajramFacetIndex.getInstance(project);
    List<FacetInfo> dependencies = index.getDependencyFacets(vajramClass);
    String[] names = dependencies.stream().map(FacetInfo::name).toArray(String[]::new);
    int choice =
        Messages.showChooseDialog(
            project,
            "Select dependency facet to resolve:",
            "Generate Resolver Method",
            null,
            names,
            names[0]);
    if (choice < 0) {
      return;
    }
    FacetInfo dependency = dependencies.get(choice);
    String vajramId = vajramClass.getName();
    String depConstant = dependency.facetNameConstant();
    String depInputsConstant = "input_n";
    if (dependency.dependencyVajramName() != null) {
      List<FacetInfo> depInputs =
          index.getDependencyInputFacets(dependency, vajramClass.getResolveScope());
      if (!depInputs.isEmpty()) {
        depInputsConstant =
            dependency.dependencyVajramName()
                + REQUEST_SUFFIX
                + "."
                + depInputs.get(0).facetNameConstant();
      }
    }
    String sourceFacet =
        index.getFacets(vajramClass).stream()
            .filter(f -> f.kind() == FacetKind.INPUT)
            .map(FacetInfo::name)
            .findFirst()
            .orElse("input");
    String snippet =
        """
        @%s(
            dep = %s%s.%s,
            depInputs = %s)
        static %s resolve%sFor%s(%s %s) {
          return %s;
        }
        """
            .formatted(
                Resolve.class.getName(),
                vajramId,
                FACETS_CLASS_SUFFIX,
                depConstant,
                depInputsConstant,
                dependency.type().getPresentableText(),
                capitalize(dependency.name()),
                dependency.dependencyVajramName() != null
                    ? dependency.dependencyVajramName()
                    : "Dep",
                VajramPsiUtil.collectFacets(vajramClass).stream()
                    .filter(f -> f.name().equals(sourceFacet))
                    .findFirst()
                    .map(VajramPsiUtil::parameterTypeText)
                    .orElse("String"),
                sourceFacet,
                sourceFacet);
    WriteCommandAction.runWriteCommandAction(
        project,
        () -> {
          PsiElementFactory factory = PsiElementFactory.getInstance(project);
          PsiMethod method =
              factory.createMethodFromText(snippet, (PsiJavaFile) vajramClass.getContainingFile());
          vajramClass.add(method);
          CodeStyleManager.getInstance(project).reformat(vajramClass);
        });
  }

  private static String capitalize(String value) {
    if (value.isEmpty()) {
      return value;
    }
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }
}
