plugins {
    kotlin("jvm") version "2.3.21"
}

group = "org.dthv.link-forge"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}