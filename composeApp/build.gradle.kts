import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidMultiplatformLibrary)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.serialization)
	alias(libs.plugins.buildKonfig)
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
	android {
		namespace = "de.lehrbaum.firefly.shared"
		compileSdk = libs.versions.android.compileSdk.get().toInt()
		minSdk = libs.versions.android.minSdk.get().toInt()
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_17)
		}
		androidResources {
			enable = true
		}
	}

	jvm {
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_17)
		}
		val testCompilation = compilations["test"]
		val uiTestCompilation = compilations.create("uiTest") {
			associateWith(testCompilation)
		}
		testRuns.create("uiTest") {
			setExecutionSourceFrom(uiTestCompilation)
		}
	}

	wasmJs {
		outputModuleName = "composeApp"
		browser()
		binaries.executable()
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
		androidMain.dependencies {
			implementation(libs.ktor.client.cio)
		}
		commonMain.dependencies {
			implementation(libs.compose.foundation)
			implementation(libs.compose.material3)
			implementation(libs.compose.runtime)
			implementation(libs.compose.ui)
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
		jvmTest.dependencies {
			implementation(libs.junit)
		}
		named("jvmUiTest") {
			dependencies {
				implementation(libs.junit)
				implementation(libs.compose.ui.test)
				implementation(libs.compose.ui.tooling.preview)
				implementation(libs.multiplatform.settings.test)
				implementation(compose.desktop.currentOs)
			}
		}
		iosMain.dependencies {
			implementation(libs.ktor.client.darwin)
		}
		commonTest.dependencies {
			implementation(libs.kotlin.test)
			implementation(libs.kotlinx.coroutines.test)
			implementation(libs.ktor.client.mock)
		}
		named("wasmJsMain") {
			dependencies {
				implementation(libs.ktor.client.js.wasm)
				implementation(libs.ktor.client.contentNegotiation.wasm)
				implementation(libs.ktor.serialization.kotlinxJson.wasm)
				implementation(libs.multiplatform.settings.wasm.js)
				implementation(libs.napier.wasm.js)
			}
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
