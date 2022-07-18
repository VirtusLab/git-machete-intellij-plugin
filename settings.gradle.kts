pluginManagement {
    dependencyResolutionManagement {
        repositories {
            // For nl.littlerobots.version-catalog-update
            mavenCentral()
        }
    }
}

rootProject.name = "git-machete-intellij-plugin"
// Note: please keep the projects in a topological order
include("binding")
include("qual")
include("testCommon")
include("gitCore:api")
include("gitCore:jGit")
include("branchLayout:api")
include("branchLayout:impl")
include("backend:api")
include("backend:impl")
include("frontend:icons")
include("frontend:base")
include("frontend:actions")
include("frontend:file")
include("frontend:graph:api")
include("frontend:graph:impl")
include("frontend:ui:api")
include("frontend:ui:impl")
