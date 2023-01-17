import com.virtuslab.gitmachete.buildsrc.*

dependencies { api(project(":gitCore:api")) }

jgit()
junit5()
powerMock5()
lombok()
slf4jLambdaApi()
slf4jTestImpl()
vavr()

applyAliasingChecker()
