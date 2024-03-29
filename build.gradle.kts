import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven`
    `java-library`
}

apply(from = "properties.gradle.kts")

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = ext["jvmTarget"] as String
}

repositories {
    jcenter()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.github.microutils:kotlin-logging:1.7.7")
    implementation("it.unimi.dsi:fastutil:8.3.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperties = mapOf(
        "junit.jupiter.execution.parallel.enabled" to true,
        "junit.jupiter.execution.parallel.mode.default" to "concurrent"
    )
    testLogging {
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}
