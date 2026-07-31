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
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
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
    archiveBaseName.set("NekaraFishing")
}

tasks.register<Copy>("copyJarToDist") {
    dependsOn(tasks.jar)
    from(tasks.jar)
    into(layout.projectDirectory.dir("dist"))
}

tasks.register<Copy>("copyJarToRootDist") {
    dependsOn(tasks.jar)
    from(tasks.jar)
    into(layout.projectDirectory.dir("../../dist"))
}

tasks.build {
    finalizedBy("copyJarToDist", "copyJarToRootDist")
}
