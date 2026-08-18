subprojects {
    val dockerfile = file("deploy/Dockerfile")
    if (dockerfile.exists()) {
        tasks.register<Exec>("buildDockerImage") {
            group = "docker"
            dependsOn("bootJar")
            workingDir = rootDir
            commandLine(
                "docker", "build",
                "-f", dockerfile.relativeTo(rootDir).path,
                "-t", "link-forge/${project.name}",
                "."
            )
            onlyIf { !project.hasProperty("skipBuildImage") }
        }

        pluginManager.withPlugin("org.springframework.boot") {
            tasks.named("build") {
                finalizedBy("buildDockerImage")
            }
        }
    }
}