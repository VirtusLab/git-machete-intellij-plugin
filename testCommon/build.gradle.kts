import com.virtuslab.gitmachete.buildsrc.*

plugins {
  `java-library`
  `java-test-fixtures`
}

dependencies {
  betterStrings("testFixtures")
  commonsIO("testFixtures")
  junitApi("testFixtures")
  lombok("testFixtures")
}
