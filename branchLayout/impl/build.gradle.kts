import com.virtuslab.gitmachete.buildsrc.BuildUtils

dependencies { api(project(":branchLayout:api")) }

BuildUtils.junit(project)

BuildUtils.lombok(project)

BuildUtils.powerMock(project)

BuildUtils.slf4jLambdaApi(project)

BuildUtils.slf4jTestImpl(project)

BuildUtils.vavr(project)
