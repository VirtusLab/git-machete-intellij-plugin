import com.virtuslab.gitmachete.buildsrc.BuildUtils

dependencies {
  api(project(":qual"))
  api(project(":branchLayout:api"))
}

BuildUtils.lombok(project)

BuildUtils.vavr(project)
