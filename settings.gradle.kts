rootProject.name = "coassign"

fun applyIfExists(path: Any) {
    if (file(path).exists()) {
        apply(from = path)
    }
}

applyIfExists("local.settings.gradle.kts")

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