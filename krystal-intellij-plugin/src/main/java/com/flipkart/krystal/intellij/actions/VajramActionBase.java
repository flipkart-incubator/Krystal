package com.flipkart.krystal.intellij.actions;

import com.flipkart.krystal.intellij.psi.VajramPsiUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class VajramActionBase extends com.intellij.openapi.actionSystem.AnAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    PsiClass vajramClass = getVajramClass(e);
    e.getPresentation().setEnabledAndVisible(vajramClass != null && isApplicable(vajramClass));
  }

  protected abstract boolean isApplicable(@NotNull PsiClass vajramClass);

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    PsiClass vajramClass = getVajramClass(e);
    if (project == null || vajramClass == null) {
      return;
    }
    perform(project, vajramClass, e);
  }

  protected abstract void perform(
      @NotNull Project project, @NotNull PsiClass vajramClass, @NotNull AnActionEvent e);

  protected static @Nullable PsiClass getVajramClass(@NotNull AnActionEvent e) {
    PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
    if (file == null) {
      return null;
    }
    PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
    if (element == null) {
      element = file.findElementAt(e.getData(CommonDataKeys.EDITOR) != null
          ? e.getData(CommonDataKeys.EDITOR).getCaretModel().getOffset()
          : 0);
    }
    if (element == null) {
      return null;
    }
    return VajramPsiUtil.findEnclosingVajram(element);
  }
}
