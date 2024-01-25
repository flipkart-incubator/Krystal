package com.flipkart.krystal.mojo;

import static com.flipkart.krystal.mojo.PublishStage.DEV;
import static com.flipkart.krystal.mojo.PublishStage.PRODUCTION;
import static com.flipkart.krystal.mojo.PublishTarget.LOCAL;
import static com.flipkart.krystal.mojo.PublishTarget.REMOTE;
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
import com.vdurmont.semver4j.Semver;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
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
import org.gradle.api.logging.Logger;
import org.gradle.api.publish.Publication;
import org.gradle.api.publish.PublishingExtension;

final class Publisher {

  public static final String DEFAULT_VERSION = "0.0.0";
  private final Set<Project> allProjects = new LinkedHashSet<>();
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
  private @Nullable MultiProjectInfo multiProjectInfo;
  private PublishStage publishStage;
  private boolean projectScanCompleted = false;

  Publisher(Project project) {
    this.rootProject = project.getRootProject();
    scanAllProjects();
  }

  static boolean isPendingPublish(
      Project project, PublishTarget target, PublishStage... publishStage) throws IOException {
    Optional<ProjectInfo> projectInfoOpt =
        getProjectInfo(readMultiProjectInfoOrDefault(project.getRootProject()), project, false);
    return projectInfoOpt
        .map(
            projectInfo ->
                Arrays.stream(publishStage)
                    .map(projectInfo::getStageInfo)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(
                        si ->
                            si.getPendingTargets() != null
                                && si.getPendingTargets().contains(target))
                    .map(projectStageInfo -> true)
                    .findAny()
                    .orElse(false))
        .orElse(false);
  }

  static boolean isPendingPublish(Project project, PublishTarget repository) throws IOException {
    return isPendingPublish(project, repository, PublishStage.values());
  }

  void printMojoState(Project project) throws IOException, GitAPIException {
    Logger l = project.getLogger();
    MultiProjectInfo multiProjectInfo = readMultiProjectInfoOrDefault(project);
    l.lifecycle(
        "------------------------------------------------------------------------------------------");
    try (Git git =
        Git.open(new FileRepositoryBuilder().findGitDir(rootProject.getRootDir()).getGitDir())) {
      Repository repo = git.getRepository();
      ObjectId headCommit = repo.resolve("HEAD");
      String baseCommitId = multiProjectInfo.getBaseCommitId();
      l.lifecycle("HEAD commit id: {}", headCommit.name());
      l.lifecycle("Base commit id: {}", baseCommitId);
      l.lifecycle(
          "------------------------------------------------------------------------------------------");
      l.lifecycle("Diff:");
      LogCommand log = git.log();
      if (baseCommitId != null) {
        log.addRange(repo.resolve(baseCommitId), headCommit);
      }
      for (RevCommit revCommit : log.call()) {
        l.lifecycle("{} - {}", revCommit.getId().abbreviate(7).name(), revCommit.getShortMessage());
      }
    }
    l.lifecycle(
        "------------------------------------------------------------------------------------------");
    l.lifecycle("%20s | %-15s | %s".formatted("Project Name", "Production", "Development"));
    l.lifecycle(
        "------------------------------------------------------------------------------------------");
    getCurrentProjectVersions(project, multiProjectInfo)
        .forEach(
            (p, versionMap) ->
                l.lifecycle(
                    "%20s : %-15s | %s"
                        .formatted(p.getName(), versionMap.get(PRODUCTION), versionMap.get(DEV))));
    l.lifecycle(
        "------------------------------------------------------------------------------------------");
    l.lifecycle("L = Pending Local Publish, R = Pending Remote Publish");
    l.lifecycle(
        "------------------------------------------------------------------------------------------");
  }

  String getMojoVersion(Project project) throws IOException {
    Optional<ProjectInfo> projectInfo = getProjectInfo(getMultiProjectInfo(), project, false);
    if (projectInfo.isEmpty()) {
      return null;
    }
    return getCurrentProjectVersion(
            project, getPublishStage(projectInfo.get()), getMultiProjectInfo())
        .version()
        .getValue();
  }

  void mojoProjectVersions(PublishTask publishTask, PublishStage publishStage)
      throws IOException, GitAPIException {
    this.publishStage = publishStage;
    if (!isRootProject(publishTask.getProject())) {
      publishTask.getLogger().debug("This is not the root project, so this is a no-op");
      return;
    }
    scanDependents();
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
        if (hasPublications(nextProjectToRelease.get())) {
          computePublishVersion(
              publishTask,
              nextProjectToRelease.get(),
              git,
              getOrCreateProjectInfo(getMultiProjectInfo(), nextProjectToRelease.get()));
        }
        nextProjectToRelease = getNextProjectReadyToPublish(projectsToPublish);
      }
      updateInfoFile(getMultiProjectInfo());
    }
  }

  boolean isRootProject(Project project) {
    return rootProject.equals(project.getProject());
  }

  void markPublished(Project project, PublishTarget publishTarget) throws IOException {
    MultiProjectInfo multiProjectInfo = readMultiProjectInfoOrDefault(project);
    Optional<ProjectInfo> projectInfoOpt = getProjectInfo(multiProjectInfo, project, false);
    if (projectInfoOpt.isEmpty()) {
      return;
    }
    ProjectInfo projectInfo = projectInfoOpt.get();
    projectInfo
        .getStageInfo(getPublishStage(projectInfo))
        .ifPresent(projectStageInfo -> projectStageInfo.getPendingTargets().remove(publishTarget));
    updateInfoFile(multiProjectInfo);
  }

  private Map<Project, Map<PublishStage, PendingVersion>> getCurrentProjectVersions(
      Project project, MultiProjectInfo multiProjectInfo) {
    Map<Project, Map<PublishStage, PendingVersion>> versionMap = new TreeMap<>();
    for (Project p :
        (Iterable<Project>)
            getAllProjects().stream().filter(Publisher::hasPublications)::iterator) {
      if (!hasPublications(p)) {
        continue;
      }
      if (isRootProject(project) || p.equals(project)) {
        for (PublishStage publishStage : PublishStage.values()) {
          versionMap
              .computeIfAbsent(p, _k -> new LinkedHashMap<>())
              .put(publishStage, getCurrentProjectVersion(p, publishStage, multiProjectInfo));
        }
      }
    }
    return versionMap;
  }

  private Set<Project> getAllProjects() {
    return allProjects;
  }

  private MultiProjectInfo getMultiProjectInfo() throws IOException {
    if (multiProjectInfo == null) {
      this.multiProjectInfo = readMultiProjectInfoOrDefault(rootProject);
    }
    return multiProjectInfo;
  }

  private void updateInfoFile(MultiProjectInfo multiProjectInfo) throws IOException {
    OBJECT_MAPPER.writeValue(getProjectInfoAbsolutePath(rootProject).toFile(), multiProjectInfo);
    this.multiProjectInfo = multiProjectInfo;
  }

  private PublishStage getPublishStage(ProjectInfo projectInfo) {
    return publishStage != null
        ? publishStage
        : projectInfo.getStageInfos().stream()
            .max(
                comparing(
                    projectStageInfo ->
                        Optional.ofNullable(projectStageInfo.getPublishTime()).orElse(Instant.MIN)))
            .orElseThrow()
            .getStage();
  }

  private static PendingVersion getCurrentProjectVersion(
      Project project, PublishStage publishStage, MultiProjectInfo multiProjectInfo) {
    ProjectInfo projectInfo = getProjectInfo(multiProjectInfo, project, false).orElseThrow();
    Optional<ProjectStageInfo> projectStageInfo =
        projectInfo.getStageInfo(publishStage).or(() -> projectInfo.getStageInfo(PRODUCTION));
    return new PendingVersion(
        new Semver(projectStageInfo.map(ProjectStageInfo::getVersion).orElse(DEFAULT_VERSION)),
        projectStageInfo.map(ProjectStageInfo::getPendingTargets).orElse(new TreeSet<>()));
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

  private void scanAllProjects() {
    if (projectScanCompleted) {
      return;
    }
    _scanAllProjects(rootProject);
    this.projectScanCompleted = true;
  }

  private void _scanAllProjects(Project project) {
    allProjects.add(project);
    absolutePathToProject.put(project.getProjectDir().toPath(), project);
    project.getSubprojects().forEach(this::_scanAllProjects);
  }

  private static boolean hasPublications(Project project) {
    return getPublications(project).findAny().isPresent();
  }

  private static Stream<Publication> getPublications(Project project) {
    return Optional.ofNullable(project.getExtensions().findByType(PublishingExtension.class))
        .map(PublishingExtension::getPublications)
        .stream()
        .flatMap(Collection::stream);
  }

  private Set<Project> findProjectsToPublish(PublishStage publishStage, Git git)
      throws IOException, GitAPIException {

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
      ObjectReader reader = git.getRepository().newObjectReader();
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

  private void scanDependents() {
    for (Project project : getAllProjects()) {
      ConfigurationContainer configurations = project.getConfigurations();
      Set<Project> allProjectDependencies =
          configurations.getAsMap().entrySet().stream()
              .filter(e -> !e.getKey().startsWith("test"))
              .map(Entry::getValue)
              .map(Configuration::getAllDependencies)
              .<Set<ProjectDependency>>map(d -> d.withType(ProjectDependency.class))
              .flatMap(Collection::stream)
              .map(ProjectDependency::getDependencyProject)
              // to avoid cases where the project depends on itself for runtime image creation or
              // any other tasks. Cyclic dependencies need not to be checked here as that would
              // be taken care of by gradle itslef.
              .filter(p -> !p.equals(project))
              .collect(Collectors.toSet());
      projectDependencies.put(project, allProjectDependencies);
      allProjectDependencies.forEach(
          p -> projectToDependendents.computeIfAbsent(p, _p -> new LinkedHashSet<>()).add(project));
    }
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
    Semver currentVersion =
        getCurrentProjectVersion(project, publishStage, getMultiProjectInfo()).version();
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
    projectStageInfo.setPendingTargets(Set.of(LOCAL, REMOTE));
    if (PRODUCTION.equals(publishStage)) {
      getMultiProjectInfo().setBaseCommitId(commitId);
      projectInfo.getStageInfos().removeIf(stageInfo -> DEV.equals(stageInfo.getStage()));
    }
    project
        .getLogger()
        .lifecycle("Publishing {} with version {}", project.getDisplayName(), semVerString);
  }

  private static ProjectInfo getOrCreateProjectInfo(
      MultiProjectInfo multiProjectInfo, Project project) {
    return getProjectInfo(multiProjectInfo, project, true).orElseThrow();
  }

  private static Optional<ProjectInfo> getProjectInfo(
      MultiProjectInfo multiProjectInfo, Project project, boolean createIfAbsent) {
    String projectName = project.getName();
    List<ProjectInfo> projects = multiProjectInfo.getProjects();
    ProjectInfo projectInfo =
        projects.stream().filter(p -> projectName.equals(p.getName())).findAny().orElse(null);
    if (projectInfo == null && createIfAbsent) {
      projectInfo = new ProjectInfo(projectName);
      projectInfo
          .getStageInfos()
          .add(
              new ProjectStageInfo(
                  PRODUCTION, DEFAULT_VERSION, null, Instant.now(), new TreeSet<>()));
      projects.add(projectInfo);
    }
    return Optional.ofNullable(projectInfo);
  }

  private static MultiProjectInfo readMultiProjectInfoOrDefault(Project project)
      throws IOException {
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

  private record PendingVersion(Semver version, Set<PublishTarget> pendingTargets) {
    @Override
    public String toString() {
      return version
          + " "
          + pendingTargets.stream()
              .map(PublishTarget::shortString)
              .collect(Collectors.joining(",", "[", "]"))
              .replace("[]", "");
    }
  }
}
