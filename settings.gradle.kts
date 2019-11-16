rootProject.name = "coassign"

pluginManagement {
    apply(from = "properties.gradle.kts")
    val kotlinVersion: String by extra

    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace?.startsWith("org.jetbrains.kotlin") == true) {
                useVersion(kotlinVersion)
            }
        }
    }
}