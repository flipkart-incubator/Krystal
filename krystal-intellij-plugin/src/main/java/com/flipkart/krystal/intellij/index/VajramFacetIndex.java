package com.flipkart.krystal.intellij.index;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants.REQUEST_SUFFIX;

import com.flipkart.krystal.intellij.psi.VajramPsiUtil;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.List;
import java.util.Optional;

@Service(Service.Level.PROJECT)
public final class VajramFacetIndex {

  private final Project project;

  public VajramFacetIndex(Project project) {
    this.project = project;
  }

  public static VajramFacetIndex getInstance(Project project) {
    return project.getService(VajramFacetIndex.class);
  }

  public List<FacetInfo> getFacets(PsiClass vajramClass) {
    return VajramPsiUtil.collectFacets(vajramClass);
  }

  public List<FacetInfo> getOutputAvailableFacets(PsiClass vajramClass) {
    return getFacets(vajramClass);
  }

  public List<FacetInfo> getDependencyFacets(PsiClass vajramClass) {
    return getFacets(vajramClass).stream().filter(f -> f.kind() == FacetKind.DEPENDENCY).toList();
  }

  public Optional<FacetInfo> findDependencyFacet(PsiClass vajramClass, String facetName) {
    return getDependencyFacets(vajramClass).stream()
        .filter(f -> f.name().equals(facetName))
        .findFirst();
  }

  public List<FacetInfo> getDependencyInputFacets(
      FacetInfo dependencyFacet, GlobalSearchScope scope) {
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
      VajramInfo vajramInfo, GlobalSearchScope scope) {
    String qualifiedName = vajramInfo.vajramClass().getQualifiedName();
    if (qualifiedName == null) {
      return List.of();
    }
    String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
    String facetsFqn = packageName + "." + vajramInfo.facetsClassName();
    return VajramPsiUtil.collectGeneratedStringConstants(project, scope, facetsFqn);
  }

  public List<String> getGeneratedRequestInputConstants(
      String dependencyVajramName, PsiClass currentVajram, GlobalSearchScope scope) {
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

  public List<PsiClass> getAllVajrams(GlobalSearchScope scope) {
    return VajramPsiUtil.findAllVajrams(project, scope);
  }
}
