package com.flipkart.krystal.intellij.actions;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INTERNAL_FACETS_CLASS;

import com.flipkart.krystal.intellij.psi.VajramPsiUtil;
import jakarta.inject.Inject;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;

public final class AddInjectionAction extends VajramActionBase {

  @Override
  protected boolean isApplicable(@NotNull PsiClass vajramClass) {
    return VajramPsiUtil.isVajram(vajramClass);
  }

  @Override
  protected void perform(
      @NotNull Project project, @NotNull PsiClass vajramClass, @NotNull AnActionEvent e) {
    String fieldName =
        Messages.showInputDialog(project, "Injection facet name:", "Add Injection", null, "log", null);
    if (fieldName == null || fieldName.isBlank()) {
      return;
    }
    String type =
        Messages.showInputDialog(
            project, "Injection type:", "Add Injection", null, "java.lang.System.Logger", null);
    if (type == null || type.isBlank()) {
      return;
    }
    PsiClass internalFacets =
        VajramPsiUtil.findNestedClass(vajramClass, _INTERNAL_FACETS_CLASS);
    if (internalFacets == null) {
      Messages.showErrorDialog(project, "No _InternalFacets nested type found.", "Add Injection");
      return;
    }
    String snippet =
        """
        @%s
        %s %s;
        """
            .formatted(Inject.class.getName(), type, fieldName);
    WriteCommandAction.runWriteCommandAction(
        project,
        () -> {
          PsiElementFactory factory = PsiElementFactory.getInstance(project);
          PsiField field =
              factory.createFieldFromText(snippet, (PsiJavaFile) internalFacets.getContainingFile());
          internalFacets.add(field);
          CodeStyleManager.getInstance(project).reformat(internalFacets);
        });
  }
}
