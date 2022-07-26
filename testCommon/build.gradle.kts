import com.virtuslab.gitmachete.buildsrc.BuildUtils

plugins {
    `java-library`
    `java-test-fixtures`
}

BuildUtils.lombok(project)

dependencies {
    testFixturesAnnotationProcessor(rootProject.libs.lombok)
    testFixturesAnnotationProcessor(rootProject.libs.betterStrings)
    testFixturesCompileOnly(rootProject.libs.lombok)
    testFixturesImplementation(rootProject.libs.junit)
    testFixturesImplementation(rootProject.libs.commonsIO)
}
