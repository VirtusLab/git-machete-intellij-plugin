import com.virtuslab.gitmachete.buildsrc.BuildUtils

dependencies { api(project(":qual")) }

BuildUtils.lombok(project)

BuildUtils.vavr(project)
