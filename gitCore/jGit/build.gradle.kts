import com.virtuslab.gitmachete.buildsrc.*

dependencies { api(project(":gitCore:api")) }

jgit()
lombok()
slf4jLambdaApi()
vavr()

applyAliasingChecker()
