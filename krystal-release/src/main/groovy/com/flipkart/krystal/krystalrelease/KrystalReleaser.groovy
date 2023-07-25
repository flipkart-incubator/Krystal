package com.flipkart.krystal.krystalrelease

import org.gradle.api.Plugin
import org.gradle.api.Project

import static com.flipkart.krystal.krystalrelease.KrystalReleaseUtil.getCurrentProjectVersion

final class KrystalReleaser implements Plugin<Project> {

    private KrystalReleaseUtil util = new KrystalReleaseUtil()

    void apply(Project project) {

        project.tasks.register('releaseProjectToLocal', ReleaseProjectTask) {
            group = 'krystalrelease'
            doLast {
                util.executeRelease((ReleaseProjectTask) it, ReleaseStage.DEV)
                println "Releasing to version: " + getCurrentProjectVersion(project)
            }
            finalizedBy("publishToMavenLocal")

        }

        project.tasks.register('releaseProject', ReleaseProjectTask) {
            group = 'krystalrelease'
            doLast {
                util.executeRelease((ReleaseProjectTask) it, ReleaseStage.PRODUCTION)
                println "Releasing to version: " + getCurrentProjectVersion(project)
            }
            finalizedBy("publishToMavenLocal", "publish")
        }

        project.tasks.register('printCurrentVersion') {
            group = 'krystalrelease'
            println getCurrentProjectVersion(project)
        }
    }
}