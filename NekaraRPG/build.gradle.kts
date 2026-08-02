plugins {
    java
}

group = "cz.nekara"
version = providers.gradleProperty("plugin_version").get()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:${providers.gradleProperty("purpur_api_version").get()}")
    compileOnly("io.lumine:Mythic-Dist:5.12.1") {
        isTransitive = false
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testImplementation("org.purpurmc.purpur:purpur-api:${providers.gradleProperty("purpur_api_version").get()}")
    testImplementation("com.google.code.gson:gson:2.13.2")
    testImplementation("org.yaml:snakeyaml:2.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveFileName.set("NekaraRPG.jar")
    manifest {
        attributes(
            "Implementation-Title" to "NekaraRPG",
            "Implementation-Version" to project.version
        )
    }
}

val cleanReleaseArtifacts by tasks.registering(Delete::class) {
    delete(fileTree(layout.projectDirectory.dir("dist")) {
        include("NekaraRPG*.jar")
    })
    delete(fileTree(layout.projectDirectory.dir("../../dist")) {
        include("NekaraRPG*.jar")
    })
}

tasks.register<Copy>("copyJarToDist") {
    dependsOn(tasks.build)
    dependsOn(cleanReleaseArtifacts)
    from(tasks.jar)
    into(layout.projectDirectory.dir("dist"))
}

tasks.register<Copy>("copyJarToRootDist") {
    dependsOn(tasks.build)
    dependsOn(cleanReleaseArtifacts)
    from(tasks.jar)
    into(layout.projectDirectory.dir("../../dist"))
}

tasks.register("release") {
    group = "build"
    description = "Runs all checks and publishes the stable-name NekaraRPG JAR."
    dependsOn("copyJarToDist", "copyJarToRootDist")
}
