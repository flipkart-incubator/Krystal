package com.flipkart.krystal.mojo

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration
import org.gradle.jvm.tasks.Jar

final class MojoPublishPlugin implements Plugin<Project> {

    private Publisher publisher

    void apply(Project project) {

        project.pluginManager.apply("maven-publish")

        publisher = new Publisher(project)

        project.version = publisher.getMojoVersion(project)

        project.tasks.register('mojoReleaseVersion', PublishTask) {
            group = 'mojo'
            doFirst {
                publisher.executePublish((PublishTask) it, PublishStage.PRODUCTION)
            }
        }

        project.tasks.register('mojoSnapshotVersion', PublishTask) {
            group = 'mojo'
            doFirst {
                publisher.executePublish((PublishTask) it, PublishStage.DEV)
            }
        }

        project.tasks.register('updatePublicationVersions') {
            doLast {
                publisher.updatePublicationVersions(project)
            }
        }

        project.tasks.register('printCurrentMojoVersions') {
            group = 'mojo'
            doLast {
                if (!publisher.isRootProject(project)) {
                    project.logger.debug("Not printing mojo versions since this is not the root project")
                    return
                }
                project.logger.lifecycle("PRODUCTION Versions")
                project.logger.lifecycle("___________________")
                publisher.getCurrentProjectVersions(PublishStage.PRODUCTION).forEach { p, version ->
                    project.logger.lifecycle("  {} : {} ", p.getDisplayName(), version)
                }

                project.logger.lifecycle("DEV Versions")
                project.logger.lifecycle("____________")
                publisher.getCurrentProjectVersions(PublishStage.DEV).forEach { p, version ->
                    project.logger.lifecycle("  {} : {} ", p.getDisplayName(), version)
                }
            }
        }

    }
}