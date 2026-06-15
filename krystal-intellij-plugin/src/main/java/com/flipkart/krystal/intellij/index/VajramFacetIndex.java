package com.flipkart.krystal.intellij.index;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants.REQUEST_SUFFIX;

import com.flipkart.krystal.intellij.psi.VajramPsiUtil;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class VajramFacetIndex {

  private final Project project;

  public VajramFacetIndex(@NotNull Project project) {
    this.project = project;
  }

  public static VajramFacetIndex getInstance(@NotNull Project project) {
    return project.getService(VajramFacetIndex.class);
  }

  public List<FacetInfo> getFacets(@NotNull PsiClass vajramClass) {
    return VajramPsiUtil.collectFacets(vajramClass);
  }

  public List<FacetInfo> getOutputAvailableFacets(@NotNull PsiClass vajramClass) {
    return getFacets(vajramClass);
  }

  public List<FacetInfo> getDependencyFacets(@NotNull PsiClass vajramClass) {
    return getFacets(vajramClass).stream()
        .filter(f -> f.kind() == FacetKind.DEPENDENCY)
        .toList();
  }

  public Optional<FacetInfo> findDependencyFacet(
      @NotNull PsiClass vajramClass, @NotNull String facetName) {
    return getDependencyFacets(vajramClass).stream()
        .filter(f -> f.name().equals(facetName))
        .findFirst();
  }

  public List<FacetInfo> getDependencyInputFacets(
      @NotNull FacetInfo dependencyFacet, @NotNull GlobalSearchScope scope) {
    if (dependencyFacet.dependencyVajramName() == null) {
      return List.of();
    }
    PsiClass depVajram =
        com.intellij.psi.JavaPsiFacade.getInstance(project)
            .findClass(dependencyFacet.dependencyVajramName(), scope);
    if (depVajram == null) {
      return List.of();
    }
    return VajramPsiUtil.collectFacets(depVajram).stream()
        .filter(f -> f.kind() == FacetKind.INPUT)
        .toList();
  }

  public List<String> getGeneratedFacetNameConstants(
      @NotNull VajramInfo vajramInfo, @NotNull GlobalSearchScope scope) {
    String qualifiedName = vajramInfo.vajramClass().getQualifiedName();
    if (qualifiedName == null) {
      return List.of();
    }
    String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
    String facetsFqn = packageName + "." + vajramInfo.facetsClassName();
    return VajramPsiUtil.collectGeneratedStringConstants(project, scope, facetsFqn);
  }

  public List<String> getGeneratedRequestInputConstants(
      @NotNull String dependencyVajramName,
      @NotNull PsiClass currentVajram,
      @NotNull GlobalSearchScope scope) {
    PsiClass depVajram =
        com.intellij.psi.JavaPsiFacade.getInstance(project).findClass(dependencyVajramName, scope);
    if (depVajram == null) {
      return List.of();
    }
    String qualifiedName = depVajram.getQualifiedName();
    if (qualifiedName == null) {
      return List.of();
    }
    String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
    String requestFqn = packageName + "." + dependencyVajramName + REQUEST_SUFFIX;
    return VajramPsiUtil.collectGeneratedStringConstants(project, scope, requestFqn);
  }

  public List<PsiClass> getAllVajrams(@NotNull GlobalSearchScope scope) {
    return VajramPsiUtil.findAllVajrams(project, scope);
  }
}
