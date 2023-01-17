import com.virtuslab.gitmachete.buildsrc.*

plugins {
  `java-library`
  `java-test-fixtures`
}

dependencies {
  testFixturesAnnotationProcessor(rootProject.libs.lombok)
  testFixturesAnnotationProcessor(rootProject.libs.betterStrings)
  testFixturesCompileOnly(rootProject.libs.lombok)
  testFixturesImplementation(rootProject.libs.junit)
  testFixturesImplementation(rootProject.libs.commonsIO)
}
