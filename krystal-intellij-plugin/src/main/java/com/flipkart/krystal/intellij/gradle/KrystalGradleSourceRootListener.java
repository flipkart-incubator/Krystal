package com.flipkart.krystal.intellij.gradle;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiManager;
import java.nio.file.Path;
import java.util.List;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Marks Krystal Gradle generated source directories when they appear on disk. */
public final class KrystalGradleSourceRootListener implements ProjectActivity {

  private static final String KRYSTAL_MODELS_GEN =
      "build/generated/sources/krystalModels/java/main";
  private static final String ANNOTATION_PROCESSOR_GEN =
      "build/generated/sources/annotationProcessor/java/main";

  @Override
  public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> $completion) {
    project.getMessageBus()
        .connect()
        .subscribe(
            VirtualFileManager.VFS_CHANGES,
            new BulkFileListener() {
              @Override
              public void after(@NotNull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                  VirtualFile file = event.getFile();
                  if (file != null) {
                    maybeRefreshGeneratedRoots(project, file);
                  }
                }
              }
            });
    return Unit.INSTANCE;
  }

  private static void maybeRefreshGeneratedRoots(Project project, VirtualFile file) {
    String path = file.getPath();
    if (path.contains(KRYSTAL_MODELS_GEN) || path.contains(ANNOTATION_PROCESSOR_GEN)) {
      PsiManager.getInstance(project).dropPsiCaches();
    }
  }
}
