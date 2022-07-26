import com.virtuslab.gitmachete.buildsrc.BuildUtils

dependencies {
    api(project(":qual"))
    api(project(":backend:api"))
}

BuildUtils.lombok(project)

BuildUtils.vavr(project)

BuildUtils.addIntellijToCompileClasspath(project, mapOf("withGit4Idea" to false))
