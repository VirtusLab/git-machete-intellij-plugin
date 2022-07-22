plugins {
    `java-library`
    `java-test-fixtures`
}
//val commonsIO: Unit by extra
dependencies {
            testFixturesImplementation(rootProject.libs.commonsIO)
        }

//val junit: Unit by extra
dependencies {
            testFixturesImplementation(rootProject.libs.junit)
        }

//t0d0 Can't be replaced with lombok() as before because of the testFixture difference - either add fixtures to lombok() or write here inline
//val lombok: Unit by extra
dependencies {
            compileOnly(rootProject.libs.lombok)
            annotationProcessor(rootProject.libs.lombok)
            testFixturesCompileOnly(rootProject.libs.lombok)
            testFixturesAnnotationProcessor(rootProject.libs.lombok)
        }

dependencies {
    testFixturesAnnotationProcessor(rootProject.libs.betterStrings)
}
