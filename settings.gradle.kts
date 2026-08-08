rootProject.name = "link-forge"

include("app")
include("domain")
include("utils")

project(":app").projectDir = file("src/app")
project(":domain").projectDir = file("src/domain")
project(":utils").projectDir = file("src/utils")