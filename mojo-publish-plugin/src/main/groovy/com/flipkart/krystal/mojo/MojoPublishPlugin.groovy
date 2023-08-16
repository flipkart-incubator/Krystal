package com.flipkart.krystal.mojo

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.TaskProvider

import static com.flipkart.krystal.mojo.PublishStage.DEV
import static com.flipkart.krystal.mojo.PublishStage.PRODUCTION
import static com.flipkart.krystal.mojo.PublishTarget.LOCAL
import static com.flipkart.krystal.mojo.PublishTarget.REMOTE

import static com.flipkart.krystal.mojo.Publisher.isPendingPublish

final class MojoPublishPlugin implements Plugin<Project> {

    private Publisher publisher

    void apply(Project project) {

        project.pluginManager.apply("maven-publish")

        publisher = new Publisher(project)

        project.version = publisher.getMojoVersion(project)

        project.tasks.register('mojoDevVersion', PublishTask) {
            group = 'mojo'
            doFirst {
                publisher.mojoProjectVersions((PublishTask) it, DEV)
            }
            finalizedBy("printMojoState")
        }

        project.tasks.register('mojoProductionVersion', PublishTask) {
            group = 'mojo'
            doFirst {
                publisher.mojoProjectVersions((PublishTask) it, PRODUCTION)
            }
            finalizedBy("printMojoState")
        }

        project.tasks.withType(PublishToMavenLocal).configureEach {
            onlyIf {
                def publish = isPendingPublish(project, LOCAL)
                if (!publish) {
                    logger.lifecycle("This task has been disabled as {} does not have any pending local publish.", project)
                }
                return publish
            }
            doLast {
                publisher.markPublished(project, LOCAL)
            }
        }

        project.tasks.withType(PublishToMavenRepository).configureEach {
            onlyIf {
                def publish = isPendingPublish(project, REMOTE)
                if (!publish) {
                    logger.lifecycle("This task has been disabled as {} does not have any pending remote publish.", project)
                }
                return publish
            }
            doLast {
                publisher.markPublished(project, REMOTE)
            }
        }

        project.tasks.register('printMojoState') {
            group = 'mojo'
            dependsOn childTasks(name, project)
            doLast {
                if (!publisher.isRootProject(project)) {
                    project.logger.debug("Not printing mojo versions since this is not the root project")
                    return
                }
                publisher.printMojoState(project)
            }
        }

        project.tasks.named('publishToMavenLocal').configure {
            dependsOn childTasks(name, project)
            finalizedBy('printMojoState')
        }

        project.tasks.named('publish').configure {
            dependsOn childTasks(name, project)
            finalizedBy('printMojoState')
        }
    }

    private static Set<TaskProvider> childTasks(String name, Project project) {
        project.subprojects.collect { it.tasks }*.named(name)
    }
}