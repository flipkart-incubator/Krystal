package com.flipkart.krystal.krystalrelease;

import static com.flipkart.krystal.krystalrelease.ReleaseStage.PRODUCTION;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.Sets;
import com.vdurmont.semver4j.Semver;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven;

final class KrystalReleaseUtil {

  private final Set<Project> projectsToRelease = new LinkedHashSet<>();
  private final Map<Project, Set<Project>> projectDependencies = new LinkedHashMap<>();
  private final Map<Project, Set<Project>> projectToDependendents = new LinkedHashMap<>();

  private static final ObjectMapper OBJECT_MAPPER =
      new YAMLMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
          .setSerializationInclusion(Include.NON_EMPTY);

  static Semver getCurrentProjectVersion(Project project) throws IOException {
    return new Semver(
        getProjectInfoOrDefault(
                readRepoInfoOrDefault(
                    new FileRepositoryBuilder().findGitDir(project.getRootDir()).getGitDir()),
                project)
            .getVersion());
  }

  void executeRelease(ReleaseProjectTask releaseTask, ReleaseStage releaseStage)
      throws IOException {
    Project project = releaseTask.getProject();
    findAllProjects(project.getRootProject());
    findProjectsToRelease(project);
    try (Repository repo = new FileRepositoryBuilder().findGitDir(project.getRootDir()).build()) {
      Optional<Project> nextProjectToRelease = getNextProjectToRelease();
      File repoInfoFile = getDefaultRepoInfoFilePath(repo.getDirectory()).toFile();
      RepoInfo repoInfo = readRepoInfoOrDefault(repo.getDirectory());
      while (nextProjectToRelease.isPresent()) {
        incrementProjectVersion(
            releaseTask,
            releaseStage,
            nextProjectToRelease.get(),
            repo,
            getProjectInfoOrDefault(repoInfo, nextProjectToRelease.get()));
        nextProjectToRelease = getNextProjectToRelease();
      }
      OBJECT_MAPPER.writeValue(repoInfoFile, repoInfo);
    }
  }

  private Optional<Project> getNextProjectToRelease() {
    Optional<Project> project =
        projectsToRelease.stream().filter(p -> !hasDependencyPendingRelease(p)).findAny();
    project.ifPresent(projectsToRelease::remove);
    return project;
  }

  private boolean hasDependencyPendingRelease(Project project) {
    return projectDependencies.getOrDefault(project, Set.of()).stream()
        .anyMatch(p -> projectsToRelease.contains(p) || hasDependencyPendingRelease(p));
  }

  private void findAllProjects(Project rootProject) {
    ConfigurationContainer configurations = rootProject.getConfigurations();
    Set<ProjectDependency> dependencies =
        Sets.union(
            Optional.ofNullable(configurations.findByName("implementation"))
                .map(Configuration::getAllDependencies)
                .<Set<ProjectDependency>>map(d -> d.withType(ProjectDependency.class))
                .orElse(Set.of()),
            Optional.ofNullable(configurations.findByName("api"))
                .map(Configuration::getAllDependencies)
                .<Set<ProjectDependency>>map(d -> d.withType(ProjectDependency.class))
                .orElse(Set.of()));
    projectDependencies.put(
        rootProject,
        dependencies.stream()
            .map(ProjectDependency::getDependencyProject)
            .collect(Collectors.toSet()));
    dependencies.forEach(
        p ->
            projectToDependendents
                .computeIfAbsent(p.getDependencyProject(), _p -> new LinkedHashSet<>())
                .add(rootProject));

    rootProject.getSubprojects().forEach(this::findAllProjects);
  }

  private void findProjectsToRelease(Project project) {
    projectsToRelease.add(project);
    projectToDependendents.getOrDefault(project, Set.of()).forEach(this::findProjectsToRelease);
  }

  private static void incrementProjectVersion(
      ReleaseProjectTask releaseTask,
      ReleaseStage releaseStage,
      Project project,
      Repository repo,
      ProjectInfo projectInfo)
      throws IOException {
    Semver currentVersion = getCurrentProjectVersion(project);
    ReleaseStage prevReleaseStage = projectInfo.getReleaseStage();
    boolean shouldIncrementVersion = PRODUCTION.equals(prevReleaseStage);
    Semver nextVersion =
        shouldIncrementVersion
            ? releaseTask.getReleaseLevel().toNextVersion(currentVersion)
            : currentVersion.withClearedSuffixAndBuild();
    String featureName = releaseTask.getFeatureName();
    if (featureName == null) {
      featureName = repo.getBranch();
    }
    String semVerString = releaseStage.decorateVersion(nextVersion, featureName).getValue();
    updatePublishTaskVersions(project, semVerString);
    projectInfo.setVersion(semVerString);
    projectInfo.setReleaseStage(releaseStage);
    projectInfo.setCommitId(repo.resolve("HEAD").name());
  }

  private static void updatePublishTaskVersions(Project project, String versionString) {
    project
        .getTasks()
        .withType(AbstractPublishToMaven.class)
        .forEach(
            publishTask -> {
              publishTask.getPublication().setVersion(versionString);
            });
  }

  private static ProjectInfo getProjectInfoOrDefault(RepoInfo repoInfo, Project project) {
    String projectName = project.getName();
    List<ProjectInfo> projects = repoInfo.getProjects();
    ProjectInfo projectInfo =
        projects.stream().filter(p -> projectName.equals(p.getName())).findAny().orElse(null);
    if (projectInfo == null) {
      projectInfo = new ProjectInfo();
      projectInfo.setName(projectName);
      projects.add(projectInfo);
    }
    return projectInfo;
  }

  private static RepoInfo readRepoInfoOrDefault(File gitDir) throws IOException {
    File repoInfoFile = getDefaultRepoInfoFilePath(gitDir).toFile();
    if (repoInfoFile.exists()) {
      return OBJECT_MAPPER.readValue(repoInfoFile, RepoInfo.class);
    } else {
      return new RepoInfo();
    }
  }

  private static Path getDefaultRepoInfoFilePath(File gitDir) {
    return gitDir.getParentFile().toPath().resolve("repo_info.krystalrelease.yaml");
  }

  KrystalReleaseUtil() {}
}
