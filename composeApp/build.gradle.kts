import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidApplication) apply false
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.serialization)
	alias(libs.plugins.buildKonfig)
}

val androidEnabled = providers
	.gradleProperty("firefly.enableAndroid")
	.map(String::toBoolean)
	.orElse(false)

if (androidEnabled.get()) {
	pluginManager.apply(libs.plugins.androidApplication.get().pluginId)
}

kotlin {
	if (androidEnabled.get()) {
		androidTarget {
			compilerOptions {
				jvmTarget.set(JvmTarget.JVM_11)
			}
		}
	}

	jvm {
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_11)
		}
		val testCompilation = compilations["test"]
		val uiTestCompilation = compilations.create("uiTest") {
			associateWith(testCompilation)
		}
		testRuns {
			val uiTest by creating {
				setExecutionSourceFrom(uiTestCompilation)
			}
		}
	}

	listOf(
		iosArm64(),
		iosSimulatorArm64(),
	).forEach { iosTarget ->
		iosTarget.binaries.framework {
			baseName = "ComposeApp"
			isStatic = true
		}
	}

	sourceSets {
		if (androidEnabled.get()) {
			val androidMain by getting
			androidMain.dependencies {
				implementation(compose.preview)
				implementation(libs.androidx.activity.compose)
				implementation(libs.ktor.client.cio)
			}
		}
		commonMain.dependencies {
			implementation(compose.components.resources)
			implementation(compose.components.uiToolingPreview)
			implementation(compose.foundation)
			implementation(compose.runtime)
			implementation(compose.ui)
			implementation(libs.compose.material3)
			implementation(libs.kotlinx.collections.immutable)
			implementation(libs.kotlinx.datetime)
			implementation(libs.ktor.client.contentNegotiation)
			implementation(libs.ktor.client.core)
			implementation(libs.ktor.serialization.kotlinxJson)
			implementation(libs.multiplatform.settings)
			implementation(libs.multiplatform.settings.noarg)
			implementation(libs.napier)
		}
		jvmMain.dependencies {
			implementation(compose.desktop.currentOs)
			implementation(libs.ktor.client.cio)
		}
		val jvmTest by getting {
			dependencies {
				implementation(libs.junit)
			}
		}
		val jvmUiTest by getting {
			dependencies {
				implementation(libs.junit)
				@OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
				implementation(compose.uiTest)
				@OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
				implementation(compose.desktop.uiTestJUnit4)
				implementation(libs.multiplatform.settings.test)
			}
		}
		iosArm64Main.dependencies {
			implementation(libs.ktor.client.darwin)
		}
		iosSimulatorArm64Main.dependencies {
			implementation(libs.ktor.client.darwin)
		}
		commonTest.dependencies {
			implementation(libs.kotlin.test)
			implementation(libs.kotlinx.coroutines.test)
			implementation(libs.ktor.client.mock)
		}
	}
}

compose.desktop {
	application {
		mainClass = "de.lehrbaum.firefly.DesktopAppKt"
	}
}

buildkonfig {
	packageName = "de.lehrbaum.firefly"

	defaultConfigs {
		val baseUrl = System.getenv("BASE_URL") ?: project.findProperty("firefly.base-url")?.toString() ?: ""
		val accessToken = System.getenv("ACCESS_TOKEN") ?: project.findProperty("firefly.access-token")?.toString() ?: ""
		buildConfigField(STRING, "BASE_URL", baseUrl)
		buildConfigField(STRING, "ACCESS_TOKEN", accessToken)
	}
}

if (androidEnabled.get()) {
	apply(from = file("android.gradle.kts"))
}
