import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  implementation(project(":binding"))

  api(project(":qual"))
  api(project(":branchLayout:api"))
}

lombok()
vavr()
