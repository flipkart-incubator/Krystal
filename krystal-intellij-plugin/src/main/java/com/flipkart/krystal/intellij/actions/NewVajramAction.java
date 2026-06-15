package com.flipkart.krystal.intellij.actions;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public final class NewVajramAction extends com.intellij.openapi.actionSystem.AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    PsiDirectory directory = getTargetDirectory(e);
    if (project == null || directory == null) {
      return;
    }
    String name =
        com.intellij.openapi.ui.Messages.showInputDialog(
            project, "Vajram class name:", "New Vajram", null, "MyVajram", null);
    if (name == null || name.isBlank()) {
      return;
    }
    String packageName =
        com.intellij.psi.JavaDirectoryService.getInstance().getPackage(directory).getQualifiedName();
    String template =
        """
        package %s;

        import com.flipkart.krystal.model.IfAbsent;
        import com.flipkart.krystal.vajram.ComputeVajramDef;
        import com.flipkart.krystal.vajram.Vajram;
        import com.flipkart.krystal.vajram.facets.Output;

        import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

        @Vajram
        public abstract class %s extends ComputeVajramDef<String> {

          interface _Inputs {
            @IfAbsent(FAIL)
            String exampleInput();
          }

          interface _InternalFacets {}

          @Output
          static String output(String exampleInput) {
            return exampleInput;
          }
        }
        """
            .formatted(packageName, name);
    com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(
        project,
        "New Vajram",
        null,
        () ->
            directory.add(
                PsiFileFactory.getInstance(project)
                    .createFileFromText(name + ".java", JavaFileType.INSTANCE, template)));
  }

  private static PsiDirectory getTargetDirectory(AnActionEvent e) {
    PsiDirectory directory = e.getData(LangDataKeys.IDE_VIEW) != null
        ? e.getData(LangDataKeys.IDE_VIEW).getOrChooseDirectory()
        : null;
    if (directory != null) {
      return directory;
    }
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (file != null) {
      return PsiManager.getInstance(e.getProject()).findDirectory(file);
    }
    return null;
  }
}
