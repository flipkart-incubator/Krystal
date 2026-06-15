package com.flipkart.krystal.intellij.actions;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INPUTS_CLASS;

import com.flipkart.krystal.intellij.psi.VajramPsiUtil;
import com.flipkart.krystal.model.IfAbsent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;

public final class AddInputAction extends VajramActionBase {

  @Override
  protected boolean isApplicable(PsiClass vajramClass) {
    return VajramPsiUtil.isVajramOrTrait(vajramClass);
  }

  @Override
  protected void perform(Project project, PsiClass vajramClass, AnActionEvent e) {
    String name =
        Messages.showInputDialog(
            project,
            "Input facet name (lowerCamelCase):",
            "Add Vajram Input",
            null,
            "exampleInput",
            null);
    if (name == null || name.isBlank()) {
      return;
    }
    String type =
        Messages.showInputDialog(project, "Input type:", "Add Vajram Input", null, "String", null);
    if (type == null || type.isBlank()) {
      return;
    }
    PsiClass inputs = VajramPsiUtil.findNestedClass(vajramClass, _INPUTS_CLASS);
    if (inputs == null) {
      Messages.showErrorDialog(project, "No _Inputs nested type found.", "Add Vajram Input");
      return;
    }
    String snippet =
        """
        @%s(%s.FAIL)
        %s %s();
        """
            .formatted(IfAbsent.class.getName(), IfAbsent.IfAbsentThen.class.getName(), type, name);
    WriteCommandAction.runWriteCommandAction(
        project,
        () -> {
          PsiElementFactory factory = PsiElementFactory.getInstance(project);
          PsiMethod method =
              factory.createMethodFromText(snippet, (PsiJavaFile) inputs.getContainingFile());
          inputs.add(method);
          CodeStyleManager.getInstance(project).reformat(inputs);
        });
  }
}
