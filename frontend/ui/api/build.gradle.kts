import com.virtuslab.gitmachete.buildsrc.BuildUtils

dependencies {
  implementation(project(":binding"))

  api(project(":branchLayout:api"))
  api(project(":backend:api"))
}

BuildUtils.vavr(project)
BuildUtils.slf4jLambdaApi(project)
BuildUtils.lombok(project)
BuildUtils.addIntellijToCompileClasspath(project, withGit4Idea = true)
