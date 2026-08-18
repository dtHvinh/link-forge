plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "link-forge"

include("link-service")
include("tracking-service")
include("domain")
include("utils")

project(":link-service").projectDir = file("src/link-service")
project(":tracking-service").projectDir = file("src/tracking-service")
project(":domain").projectDir = file("src/domain")
project(":utils").projectDir = file("src/utils")