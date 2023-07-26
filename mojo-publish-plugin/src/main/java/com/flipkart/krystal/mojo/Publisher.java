package com.flipkart.krystal.mojo;

import static com.flipkart.krystal.mojo.PublishStage.DEV;
import static com.flipkart.krystal.mojo.PublishStage.PRODUCTION;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.comparing;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Sets;
import com.vdurmont.semver4j.Semver;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
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
import org.gradle.api.publish.VariantVersionMappingStrategy;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal;
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven;

final class Publisher {

  public static final String DEFAULT_VERSION = "0.0.0";
  private final Map<Project, Set<Project>> projectDependencies = new LinkedHashMap<>();
  private final Map<Project, Set<Project>> projectToDependendents = new LinkedHashMap<>();
  private final Map<Path, Project> absolutePathToProject = new LinkedHashMap<>();

  private static final ObjectMapper OBJECT_MAPPER =
      new YAMLMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
          .setSerializationInclusion(Include.NON_EMPTY)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .registerModule(new JavaTimeModule())
          .registerModule(new Jdk8Module())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final Project rootProject;
  private MultiProjectInfo multiProjectInfo;
  private PublishStage publishStage;

  Publisher(Project project) {
    this.rootProject = project.getRootProject();
    scanAllProjects(project.getRootProject());
  }

  Map<Project, Semver> getCurrentProjectVersions(PublishStage publishStage) throws IOException {
    Map<Project, Semver> map = new HashMap<>();
    for (Project p : getAllProjects()) {
      if (map.put(p, getCurrentProjectVersion(p, publishStage, getMultiProjectInfo())) != null) {
        throw new IllegalStateException("Duplicate key");
      }
    }
    return map;
  }

  private MultiProjectInfo getMultiProjectInfo() throws IOException {
    if (multiProjectInfo == null) {
      this.multiProjectInfo = readRepoInfoOrDefault(rootProject);
    }
    return multiProjectInfo;
  }

  private Set<Project> getAllProjects() {
    return projectDependencies.keySet();
  }

  String getMojoVersion(Project project) throws IOException {
    return getCurrentProjectVersion(
            project,
            publishStage != null
                ? publishStage
                : getLatestStage(getProjectInfoOrDefault(getMultiProjectInfo(), project)),
            getMultiProjectInfo())
        .getValue();
  }

  void executePublish(PublishTask publishTask, PublishStage publishStage)
      throws IOException, GitAPIException {
    this.publishStage = publishStage;
    if (!isRootProject(publishTask.getProject())) {
      publishTask.getLogger().debug("This is not the root project, so this is a no-op");
      return;
    }
    rootProject
        .getLogger()
        .lifecycle(
            "Publish destinations: {}",
            switch (publishStage) {
              case DEV -> "MavenLocal";
              case PRODUCTION -> "MavenLocal, MavenCentral";
            });
    try (Git git =
        Git.open(new FileRepositoryBuilder().findGitDir(rootProject.getRootDir()).getGitDir())) {
      validatePrePublish(git);
      Set<Project> projectsToPublish =
          new LinkedHashSet<>(findProjectsToPublish(publishStage, git));
      if (projectsToPublish.isEmpty()) {
        rootProject.getLogger().lifecycle("No projects have changed. Not publishing anything");
        return;
      }

      Optional<Project> nextProjectToRelease = getNextProjectReadyToPublish(projectsToPublish);
      while (nextProjectToRelease.isPresent()) {
        computePublishVersion(
            publishTask,
            nextProjectToRelease.get(),
            git,
            getProjectInfoOrDefault(getMultiProjectInfo(), nextProjectToRelease.get()));
        nextProjectToRelease = getNextProjectReadyToPublish(projectsToPublish);
      }
      OBJECT_MAPPER.writeValue(
          getProjectInfoAbsolutePath(rootProject).toFile(), getMultiProjectInfo());
    }
  }

  boolean isRootProject(Project project) {
    return rootProject.equals(project.getProject());
  }

  void cleanUpAfterLocalPublish(Project project) throws GitAPIException, IOException {
    if (!isRootProject(project)) {
      project.getLogger().debug("This is not a root project, hence not cleaning up");
      return;
    }
    File gitDir = new FileRepositoryBuilder().findGitDir(rootProject.getRootDir()).getGitDir();
    Path repoRoot = gitDir.toPath().getParent().toAbsolutePath();
    try (Git git = Git.open(gitDir)) {
      rootProject
          .getLogger()
          .lifecycle("Published only local... So undoing all local changes with git checkout -f");
      git.checkout()
          .setForced(true)
          .addPath(repoRoot.relativize(getProjectInfoAbsolutePath(rootProject)).toString())
          .call();
    }
  }

  void cleanUpAfterAllPublish(Project project) throws IOException, GitAPIException {
    if (!isRootProject(project)) {
      project.getLogger().debug("This is not a root project, hence not cleaning up");
      return;
    }
    try (Git git =
        Git.open(new FileRepositoryBuilder().findGitDir(rootProject.getRootDir()).getGitDir())) {
      rootProject
          .getLogger()
          .lifecycle(
              """
                Published to all repos (including remote)... So committing local changes local changes with git commit.
                You can push the changes if you prefer, or squash multiple such commits before pushing""");
      if (!git.status().call().isClean()) {
        git.commit()
            .setAll(true)
            .setMessage(
                "[Mojo AutoPublish] Updating multi_project_info.mojo.yaml with latest auto-published versions. Root version: "
                    + getCurrentProjectVersion(rootProject, publishStage, getMultiProjectInfo())
                        .getValue())
            .call();
      }
    }
  }

  private static Semver getCurrentProjectVersion(
      Project project, PublishStage publishStage, MultiProjectInfo multiProjectInfo) {
    ProjectInfo projectInfo = getProjectInfoOrDefault(multiProjectInfo, project);
    return new Semver(
        projectInfo
            .getStageInfo(publishStage)
            .map(ProjectStageInfo::getVersion)
            .or(() -> projectInfo.getStageInfo(PRODUCTION).map(ProjectStageInfo::getVersion))
            .orElse(DEFAULT_VERSION));
  }

  void updatePublicationVersions(Project project) throws IOException {
    ProjectInfo projectInfo = getProjectInfoOrDefault(getMultiProjectInfo(), project);
    updatePublishTaskVersions(
        project, projectInfo.getStageInfo(getLatestStage(projectInfo)).orElseThrow().getVersion());
  }

  private static PublishStage getLatestStage(ProjectInfo projectInfo) {
    return projectInfo.getStageInfos().stream()
        .max(
            comparing(
                projectStageInfo ->
                    Optional.ofNullable(projectStageInfo.getPublishTime()).orElse(Instant.MIN)))
        .orElseThrow()
        .getStage();
  }

  private void validatePrePublish(Git git) throws GitAPIException {
    checkArgument(
        RepositoryState.SAFE.equals(git.getRepository().getRepositoryState()),
        "Repository is not in stable state. Please finish any unfinished merge/rebase/revert etc.");
    Status status = git.status().call();
    checkArgument(
        status.isClean()
            || (status.getModified().size() == 1
                && isProjectInfoPath(Path.of(status.getModified().iterator().next()), git)
                && status.getUntracked().isEmpty()),
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

  private void scanAllProjects(Project project) {
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

    project.getSubprojects().forEach(this::scanAllProjects);
  }

  private Set<Project> findProjectsToPublish(PublishStage publishStage, Git git)
      throws IOException, GitAPIException {
    ObjectReader reader = git.getRepository().newObjectReader();

    Set<Project> projectsToPublish = new LinkedHashSet<>();
    String baseCommitId = getMultiProjectInfo().getBaseCommitId();
    if (baseCommitId == null) {
      rootProject
          .getLogger()
          .lifecycle(
              "Base commit id is missing for {} in stage {}. Force publishing all projects",
              rootProject,
              publishStage);
      projectsToPublish.addAll(getAllProjects());
    } else {
      AbstractTreeIterator baseTree =
          getCanonicalTreeParser(git.getRepository().resolve(baseCommitId), reader, git);

      AbstractTreeIterator headTree =
          getCanonicalTreeParser(git.getRepository().resolve("HEAD"), reader, git);

      git.diff().setShowNameOnly(true).setOldTree(baseTree).setNewTree(headTree).call().stream()
          .flatMap(d -> Stream.of(d.getOldPath(), d.getNewPath()))
          .map(Path::of)
          .distinct()
          .filter(path -> !isProjectInfoPath(path, git))
          .peek(path -> rootProject.getLogger().debug("Found changed file {}", path))
          .map(p -> getProjectOf(p, git))
          .peek(p -> rootProject.getLogger().debug("Resolved project for the above path: {}", p))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .distinct()
          .forEach(project -> collectDependentsTransitively(project, projectsToPublish));
    }
    return projectsToPublish;
  }

  private boolean isProjectInfoPath(Path path, Git git) {
    Path projectInfoAbsolutePath = getProjectInfoAbsolutePath(rootProject);
    return path.equals(projectInfoAbsolutePath)
        || git.getRepository()
            .getDirectory()
            .toPath()
            .getParent()
            .relativize(projectInfoAbsolutePath)
            .equals(path);
  }

  private AbstractTreeIterator getCanonicalTreeParser(
      ObjectId commitId, ObjectReader reader, Git git) throws IOException {
    try (RevWalk walk = new RevWalk(git.getRepository())) {
      RevCommit commit = walk.parseCommit(commitId);
      ObjectId treeId = commit.getTree().getId();
      return new CanonicalTreeParser(null, reader, treeId);
    }
  }

  private void collectDependentsTransitively(Project project, Set<Project> collector) {
    collector.add(project);
    for (Project p : projectToDependendents.getOrDefault(project, Set.of())) {
      collectDependentsTransitively(p, collector);
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

  private void computePublishVersion(
      PublishTask publishTask, Project project, Git git, ProjectInfo projectInfo)
      throws IOException {
    Semver currentVersion = getCurrentProjectVersion(project, publishStage, getMultiProjectInfo());
    boolean shouldIncrementVersion =
        PRODUCTION.equals(
            projectInfo
                .getStageInfo(publishStage)
                .map(ProjectStageInfo::getStage)
                .orElse(PRODUCTION));
    Semver nextVersion =
        shouldIncrementVersion
            ? publishTask.getPublishLevel().toNextVersion(currentVersion)
            : currentVersion.withClearedSuffixAndBuild();
    String featureName = publishTask.getFeatureName();
    if (featureName == null) {
      featureName = git.getRepository().getBranch();
    }
    String semVerString = publishStage.decorateVersion(nextVersion, featureName).getValue();
    ProjectStageInfo projectStageInfo =
        projectInfo
            .getStageInfo(publishStage)
            .orElseGet(
                () -> {
                  ProjectStageInfo stageInfo = new ProjectStageInfo();
                  stageInfo.setStage(publishStage);
                  projectInfo.getStageInfos().add(stageInfo);
                  return stageInfo;
                });
    projectStageInfo.setVersion(semVerString);
    String commitId = git.getRepository().resolve("HEAD").name();
    projectStageInfo.setCommitId(commitId);
    projectStageInfo.setPublishTime(Instant.now());
    if (PRODUCTION.equals(publishStage)) {
      getMultiProjectInfo().setBaseCommitId(commitId);
      projectInfo.getStageInfos().removeIf(stageInfo -> DEV.equals(stageInfo.getStage()));
    }
    project
        .getLogger()
        .lifecycle("Publishing {} with version {}", project.getDisplayName(), semVerString);
  }

  private static void updatePublishTaskVersions(Project project, String versionString) {
    project
        .getLogger()
        .lifecycle("Updating publications of {} to version {}", project, versionString);
    project.setVersion(versionString);
    project
        .getTasks()
        .withType(AbstractPublishToMaven.class)
        .forEach(
            publishTask -> {
              MavenPublication publication = publishTask.getPublication();
              publication.setVersion(versionString);
              if (publication instanceof MavenPublicationInternal internalPublication) {
                internalPublication.getMavenProjectIdentity().getVersion().set(versionString);
                internalPublication
                    .getVersionMappingStrategy()
                    .allVariants(VariantVersionMappingStrategy::fromResolutionResult);
              }
            });
  }

  private static ProjectInfo getProjectInfoOrDefault(
      MultiProjectInfo multiProjectInfo, Project project) {
    String projectName = project.getName();
    List<ProjectInfo> projects = multiProjectInfo.getProjects();
    ProjectInfo projectInfo =
        projects.stream().filter(p -> projectName.equals(p.getName())).findAny().orElse(null);
    if (projectInfo == null) {
      projectInfo = new ProjectInfo(projectName);
      projectInfo
          .getStageInfos()
          .add(new ProjectStageInfo(PRODUCTION, DEFAULT_VERSION, null, Instant.now()));
      projects.add(projectInfo);
    }
    return projectInfo;
  }

  private static MultiProjectInfo readRepoInfoOrDefault(Project project) throws IOException {
    File repoInfoFile = getProjectInfoAbsolutePath(project).toFile();
    if (repoInfoFile.exists()) {
      return OBJECT_MAPPER.readValue(repoInfoFile, MultiProjectInfo.class);
    } else {
      return new MultiProjectInfo();
    }
  }

  private static Path getProjectInfoAbsolutePath(Project project) {
    return project.getRootDir().toPath().resolve("multi_project_info.mojo.yaml");
  }
}
