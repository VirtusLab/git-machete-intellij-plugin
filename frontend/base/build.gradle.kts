import com.virtuslab.gitmachete.buildsrc.BuildUtils

dependencies {
    api(project(":qual"))
    api(project(":backend:api"))
}

BuildUtils.jetbrainsAnnotations(project)

BuildUtils.vavr(project)

BuildUtils.addIntellijToCompileClasspath(project, mapOf("withGit4Idea" to true))
