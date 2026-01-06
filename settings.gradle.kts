rootProject.name = "happyrow-core"
include("infrastructure")
include("domain")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}