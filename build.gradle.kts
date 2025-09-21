plugins {
	// this is necessary to avoid the plugins to be loaded multiple times
	// in each subproject's classloader
	alias(libs.plugins.androidApplication) apply false
	alias(libs.plugins.androidLibrary) apply false
	alias(libs.plugins.composeMultiplatform) apply false
	alias(libs.plugins.composeCompiler) apply false
	alias(libs.plugins.kotlinMultiplatform) apply false
	alias(libs.plugins.serialization) apply false
	alias(libs.plugins.buildKonfig) apply false
	alias(libs.plugins.ktlint)
}

allprojects {
	apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)

	ktlint {
		filter {
			exclude("**/generated/**")
			exclude("**/BuildKonfig.kt")
		}
	}
}

tasks.register("checkAgentsEnvironment") {
	group = "verification"
	description = "Runs all tests that are expected to pass in the agent environment"
	dependsOn(
		":composeApp:testDebugUnitTest",
		":composeApp:testReleaseUnitTest",
		":composeApp:jvmTest",
	)
	dependsOn("ktlintCheck")
	dependsOn(subprojects.map { "${it.path}:ktlintCheck" })
}
