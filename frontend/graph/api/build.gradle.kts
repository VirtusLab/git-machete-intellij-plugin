import com.virtuslab.gitmachete.buildsrc.BuildUtils

dependencies {
  api(project(":qual"))
  api(project(":backend:api"))
}

BuildUtils.lombok(project)

BuildUtils.vavr(project)

BuildUtils.addIntellijToCompileClasspath(project, withGit4Idea = false)
