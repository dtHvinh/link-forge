rootProject.name = "link-forge"

include("link-service")
include("domain")
include("utils")

project(":link-service").projectDir = file("src/link-service")
project(":domain").projectDir = file("src/domain")
project(":utils").projectDir = file("src/utils")