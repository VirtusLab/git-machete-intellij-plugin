//val commonsIO: Unit by extra
dependencies {
            implementation(rootProject.libs.commonsIO)
        }

//val junit: Unit by extra
dependencies {
            testImplementation(rootProject.libs.junit)
        }

//val lombok: Unit by extra
dependencies {
            compileOnly(rootProject.libs.lombok)
            annotationProcessor(rootProject.libs.lombok)
            testCompileOnly(rootProject.libs.lombok)
            testAnnotationProcessor(rootProject.libs.lombok)
        }
