package com.flipkart.krystal.intellij.actions;

import com.flipkart.krystal.intellij.index.FacetInfo;
import com.flipkart.krystal.intellij.index.VajramFacetIndex;
import com.flipkart.krystal.intellij.psi.VajramPsiUtil;
import com.flipkart.krystal.vajram.facets.Output;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;

public final class GenerateOutputMethodAction extends VajramActionBase {

  @Override
  protected boolean isApplicable(PsiClass vajramClass) {
    return VajramPsiUtil.isVajram(vajramClass)
        && VajramPsiUtil.findOutputMethod(vajramClass) == null;
  }

  @Override
  protected void perform(Project project, PsiClass vajramClass, AnActionEvent e) {
    String returnType =
        Messages.showInputDialog(
            project, "Output return type:", "Generate Output Method", null, "String", null);
    if (returnType == null || returnType.isBlank()) {
      return;
    }
    VajramFacetIndex index = VajramFacetIndex.getInstance(project);
    StringBuilder params = new StringBuilder();
    for (FacetInfo facet : index.getOutputAvailableFacets(vajramClass)) {
      if (!params.isEmpty()) {
        params.append(", ");
      }
      params.append(VajramPsiUtil.parameterTypeText(facet)).append(" ").append(facet.name());
    }
    String snippet =
        """
        @%s
        static %s output(%s) {
          throw new UnsupportedOperationException("TODO");
        }
        """
            .formatted(Output.class.getName(), returnType, params);
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
}
