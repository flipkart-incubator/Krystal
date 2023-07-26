package com.flipkart.krystal.mojo

import org.gradle.api.Plugin
import org.gradle.api.Project

import static Publisher.getCurrentProjectVersion


final class MojoPublishPlugin implements Plugin<Project> {

    private Publisher publisher

    void apply(Project project) {
        publisher = new Publisher(project)

        project.tasks.register('mojoReleaseToLocal', PublishTask) {
            group = 'mojo'
            doLast {
                println "Publish destination: MavenLocal"
                publisher.executePublish((PublishTask) it, PublishStage.PRODUCTION)
            }
            finalizedBy("publishToLocalAndCleanup")
        }

        project.tasks.register('mojoSnapshotToLocal', PublishTask) {
            group = 'mojo'
            doLast {
                println "Publish destination: MavenLocal"
                publisher.executePublish((PublishTask) it, PublishStage.DEV)
            }
            finalizedBy("publishToLocalAndCleanup")

        }

        project.tasks.register('mojoReleaseToAll', PublishTask) {
            group = 'mojo'
            doLast {
                println "Publish destinations: MavenLocal, MavenCentral"
                publisher.executePublish((PublishTask) it, PublishStage.PRODUCTION)
            }
            finalizedBy("publishToAllAndCleanup")
        }

        project.tasks.register('mojoSnapshotToAll', PublishTask) {
            group = 'mojo'
            doLast {
                println "Publish destinations: MavenLocal, MavenCentral"
                publisher.executePublish((PublishTask) it, PublishStage.DEV)
            }
            finalizedBy("publishToAllAndCleanup")
        }

        project.tasks.register('publishToLocalAndCleanup') {
            group = 'mojo internal'
            mustRunAfter('publishToMavenLocal')
            publisher.cleanUpAfterLocalPublish()
        }

        project.tasks.register('publishToAllAndCleanup') {
            group = 'mojo internal'
            mustRunAfter('publishToMavenLocal', 'publish')
            publisher.cleanUpAfterAllPublish()
        }

        project.tasks.register('printCurrentMojoVersion') {
            group = 'mojo'
            println getCurrentProjectVersion(project)
        }

    }
}