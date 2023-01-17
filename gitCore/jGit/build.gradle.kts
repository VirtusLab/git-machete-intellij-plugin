import com.virtuslab.gitmachete.buildsrc.*

dependencies { api(project(":gitCore:api")) }

jgit()
junit()
powerMock()
lombok()
slf4jLambdaApi()
slf4jTestImpl()
vavr()

applyAliasingChecker()
