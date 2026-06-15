package com.flipkart.krystal.intellij.actions;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INTERNAL_FACETS_CLASS;

import com.flipkart.krystal.intellij.psi.VajramPsiUtil;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;

public final class AddDependencyAction extends VajramActionBase {

  @Override
  protected boolean isApplicable(PsiClass vajramClass) {
    return VajramPsiUtil.isVajram(vajramClass);
  }

  @Override
  protected void perform(Project project, PsiClass vajramClass, AnActionEvent e) {
    String facetName =
        Messages.showInputDialog(
            project, "Dependency facet name:", "Add Dependency", null, "dep", null);
    if (facetName == null || facetName.isBlank()) {
      return;
    }
    String depVajram =
        Messages.showInputDialog(
            project,
            "Dependency vajram class (simple name):",
            "Add Dependency",
            null,
            "OtherVajram",
            null);
    if (depVajram == null || depVajram.isBlank()) {
      return;
    }
    String returnType =
        Messages.showInputDialog(
            project, "Dependency return type:", "Add Dependency", null, "Object", null);
    if (returnType == null || returnType.isBlank()) {
      return;
    }
    PsiClass internalFacets = VajramPsiUtil.findNestedClass(vajramClass, _INTERNAL_FACETS_CLASS);
    if (internalFacets == null) {
      Messages.showErrorDialog(project, "No _InternalFacets nested type found.", "Add Dependency");
      return;
    }
    String snippet =
        """
        @%s(%s.FAIL)
        @%s(onVajram = %s.class)
        %s %s();
        """
            .formatted(
                IfAbsent.class.getName(),
                IfAbsent.IfAbsentThen.class.getName(),
                Dependency.class.getName(),
                depVajram,
                returnType,
                facetName);
    WriteCommandAction.runWriteCommandAction(
        project,
        () -> {
          PsiElementFactory factory = PsiElementFactory.getInstance(project);
          PsiMethod method =
              factory.createMethodFromText(
                  snippet, (PsiJavaFile) internalFacets.getContainingFile());
          internalFacets.add(method);
          CodeStyleManager.getInstance(project).reformat(internalFacets);
        });
  }
}
