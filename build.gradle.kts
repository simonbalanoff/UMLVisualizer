plugins {
    application
    id("com.gradleup.shadow") version "8.3.9"
}

group = "org.balanoff"
version = "2.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.javaparser:javaparser-core:3.28.2")
    implementation("info.picocli:picocli:4.7.7")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

application {
    mainClass = "umlrenderer.UMLCli"
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveFileName = "umlvisualizer-all.jar"
    manifest {
        attributes["Main-Class"] = "umlrenderer.UMLCli"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
