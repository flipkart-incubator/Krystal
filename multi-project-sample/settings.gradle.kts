pluginManagement {
    repositories {
        mavenLocal()
        maven {url=uri("https://clojars.org/repo")}
        mavenCentral()
    }
}

rootProject.name = "multi-project-sample"
include("business-logic")

include("business-logic:mod1")
findProject(":business-logic:mod1")?.name = "mod1"

include("business-logic:mod2")
findProject(":business-logic:mod2")?.name = "mod2"

include("business-logic:mod3")
findProject(":business-logic:mod3")?.name = "mod3"
include("primary-project")
