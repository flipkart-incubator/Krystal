package com.flipkart.krystal.mojo;

import static com.flipkart.krystal.mojo.PublishStage.DEV;
import static com.flipkart.krystal.mojo.PublishStage.PRODUCTION;
import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
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
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven;

final class Publisher {

  private final Map<Project, Set<Project>> projectDependencies = new LinkedHashMap<>();
  private final Map<Project, Set<Project>> projectToDependendents = new LinkedHashMap<>();
  private final Map<Path, Project> absolutePathToProject = new LinkedHashMap<>();

  private static final ObjectMapper OBJECT_MAPPER =
      new YAMLMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
          .setSerializationInclusion(Include.NON_EMPTY);
  private final Project rootProject;
  private final MultiProjectInfo multiProjectInfo;

  Publisher(Project rootProject) throws IOException {
    assert rootProject.equals(rootProject.getRootProject())
        : """
            mojo-publish plugin can only be applied on root projects.
            {%s} is not a root project. You can apply the plugin on its root project {%s} instead"""
            .formatted(rootProject, rootProject.getRootProject());
    this.rootProject = rootProject;
    this.multiProjectInfo = readRepoInfoOrDefault(rootProject);
  }

  static Semver getCurrentProjectVersion(Project project) throws IOException {
    return new Semver(
        getProjectInfoOrDefault(readRepoInfoOrDefault(project), project).getVersion());
  }

  void executePublish(PublishTask publishTask, PublishStage publishStage)
      throws IOException, GitAPIException {
    assert rootProject.equals(publishTask.getProject());
    findAllProjects(rootProject.getRootProject());

    try (Git git =
        Git.open(new FileRepositoryBuilder().findGitDir(rootProject.getRootDir()).getGitDir())) {
      validate(git);
      Set<Project> projectsToPublish =
          new LinkedHashSet<>(findProjectsToPublish(publishStage, git));
      if (projectsToPublish.isEmpty()) {
        rootProject.getLogger().lifecycle("No projects have changed. Not publishing anything");
        return;
      }

      Optional<Project> nextProjectToRelease = getNextProjectReadyToPublish(projectsToPublish);
      MultiProjectInfo multiProjectInfo = readRepoInfoOrDefault(rootProject);
      while (nextProjectToRelease.isPresent()) {
        computePublishVersion(
            publishTask,
            publishStage,
            nextProjectToRelease.get(),
            git,
            getProjectInfoOrDefault(multiProjectInfo, nextProjectToRelease.get()));
        nextProjectToRelease = getNextProjectReadyToPublish(projectsToPublish);
      }
      OBJECT_MAPPER.writeValue(getDefaultRepoInfoFilePath(rootProject).toFile(), multiProjectInfo);
    }
  }

  private void validate(Git git) throws GitAPIException {
    checkState(
        RepositoryState.SAFE.equals(git.getRepository().getRepositoryState()),
        "Repository is not in stable state. Please finish any unfinished merge/rebase/revert etc.");
    checkState(
        git.status().call().isClean(),
        """
          Cannot publish when git working tree is not clean.
          Please make sure 'git status' reports a clean working tree before mojo publish""");
  }

  private Optional<Project> getNextProjectReadyToPublish(Set<Project> projectsToPublish) {
    Optional<Project> project =
        projectsToPublish.stream()
            .filter(p -> !hasDependencyPendingRelease(p, projectsToPublish))
            .findAny();
    project.ifPresent(projectsToPublish::remove);
    return project;
  }

  private boolean hasDependencyPendingRelease(Project project, Set<Project> projectsToPublish) {
    return projectDependencies.getOrDefault(project, Set.of()).stream()
        .anyMatch(
            p ->
                projectsToPublish.contains(p) || hasDependencyPendingRelease(p, projectsToPublish));
  }

  private void findAllProjects(Project project) {
    ConfigurationContainer configurations = project.getConfigurations();
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
        project,
        dependencies.stream()
            .map(ProjectDependency::getDependencyProject)
            .collect(Collectors.toSet()));
    absolutePathToProject.put(project.getProjectDir().toPath(), project);
    dependencies.forEach(
        p ->
            projectToDependendents
                .computeIfAbsent(p.getDependencyProject(), _p -> new LinkedHashSet<>())
                .add(project));

    project.getSubprojects().forEach(this::findAllProjects);
  }

  private Set<Project> findProjectsToPublish(PublishStage publishStage, Git git)
      throws IOException, GitAPIException {
    if (DEV.equals(publishStage)) {
      return projectDependencies.keySet();
    }
    ProjectInfo projectInfo = getProjectInfoOrDefault(multiProjectInfo, rootProject);
    ObjectReader reader = git.getRepository().newObjectReader();

    String baseCommitId = projectInfo.getBaseCommitId(publishStage);
    if (baseCommitId == null) {
      baseCommitId = Iterators.getLast(git.log().call().iterator()).getName();
      rootProject
          .getLogger()
          .lifecycle(
              "Base commit id is missing for {} in stage {}. Using the oldest commit instead {}",
              rootProject,
              publishStage,
              baseCommitId);
    }
    AbstractTreeIterator baseTree =
        getCanonicalTreeParser(git.getRepository().resolve(baseCommitId), reader, git);

    AbstractTreeIterator headTree =
        getCanonicalTreeParser(git.getRepository().resolve("HEAD"), reader, git);

    Iterable<Project> changedProjects =
        git.diff().setShowNameOnly(true).setOldTree(baseTree).setNewTree(headTree).call().stream()
                .flatMap(d -> Stream.of(d.getOldPath(), d.getNewPath()))
                .map(Path::of)
                .distinct()
                .peek(path -> rootProject.getLogger().debug("Found changed file {}", path))
                .map(p -> getProjectOf(p, git))
                .peek(
                    p ->
                        rootProject.getLogger().debug("Resolved project for the above path: {}", p))
                .filter(Optional::isPresent)
                .map(Optional::get)
            ::iterator;

    Set<Project> projectsToPublish = new LinkedHashSet<>();
    for (Project changedProject : changedProjects) {
      collectDependentsTransitively(projectsToPublish, changedProject);
    }
    return projectsToPublish;
  }

  private AbstractTreeIterator getCanonicalTreeParser(
      ObjectId commitId, ObjectReader reader, Git git) throws IOException {
    try (RevWalk walk = new RevWalk(git.getRepository())) {
      RevCommit commit = walk.parseCommit(commitId);
      ObjectId treeId = commit.getTree().getId();
      return new CanonicalTreeParser(null, reader, treeId);
    }
  }

  private void collectDependentsTransitively(Set<Project> collector, Project project) {
    collector.add(project);
    for (Project p : projectToDependendents.getOrDefault(project, Set.of())) {
      collectDependentsTransitively(collector, p);
    }
  }

  private Optional<Project> getProjectOf(Path path, Git git) {
    Path repoRoot = git.getRepository().getDirectory().toPath().getParent().toAbsolutePath();
    do {
      Project project = absolutePathToProject.get(repoRoot.resolve(path));
      if (project != null) {
        return Optional.of(project);
      }
      path = path.getParent();
    } while (path != null);
    return Optional.empty();
  }

  private static void computePublishVersion(
      PublishTask publishTask,
      PublishStage publishStage,
      Project project,
      Git git,
      ProjectInfo projectInfo)
      throws IOException {
    Semver currentVersion = getCurrentProjectVersion(project);
    PublishStage prevPublishStage = projectInfo.getPublishStage();
    boolean shouldIncrementVersion = PRODUCTION.equals(prevPublishStage);
    Semver nextVersion =
        shouldIncrementVersion
            ? publishTask.getPublishLevel().toNextVersion(currentVersion)
            : currentVersion.withClearedSuffixAndBuild();
    String featureName = publishTask.getFeatureName();
    if (featureName == null) {
      featureName = git.getRepository().getBranch();
    }
    String semVerString = publishStage.decorateVersion(nextVersion, featureName).getValue();
    updatePublishTaskVersions(project, semVerString);
    projectInfo.setVersion(semVerString);
    projectInfo.setPublishStage(publishStage);
    String commitId = git.getRepository().resolve("HEAD").name();
    projectInfo.setDevBaseCommitId(commitId);
    if (PRODUCTION.equals(publishStage)) {
      projectInfo.setProductionBaseCommitId(commitId);
    }

    project
        .getLogger()
        .lifecycle("Publishing {} with version {}", project.getDisplayName(), semVerString);
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

  private static ProjectInfo getProjectInfoOrDefault(
      MultiProjectInfo multiProjectInfo, Project project) {
    String projectName = project.getName();
    List<ProjectInfo> projects = multiProjectInfo.getProjects();
    ProjectInfo projectInfo =
        projects.stream().filter(p -> projectName.equals(p.getName())).findAny().orElse(null);
    if (projectInfo == null) {
      projectInfo = new ProjectInfo();
      projectInfo.setName(projectName);
      projects.add(projectInfo);
    }
    return projectInfo;
  }

  private static MultiProjectInfo readRepoInfoOrDefault(Project project) throws IOException {
    File repoInfoFile = getDefaultRepoInfoFilePath(project).toFile();
    if (repoInfoFile.exists()) {
      return OBJECT_MAPPER.readValue(repoInfoFile, MultiProjectInfo.class);
    } else {
      return new MultiProjectInfo();
    }
  }

  private static Path getDefaultRepoInfoFilePath(Project project) {
    return project.getRootDir().toPath().resolve("multi_project_info.mojo.yaml");
  }
}
