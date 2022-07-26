import com.virtuslab.gitmachete.buildsrc.BuildUtils

dependencies {
    testImplementation(rootProject.libs.junit)
}

BuildUtils.jetbrainsAnnotations(project)
