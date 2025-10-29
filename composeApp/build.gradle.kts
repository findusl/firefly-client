import com.android.build.api.dsl.ApplicationExtension
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.serialization)
	alias(libs.plugins.buildKonfig)
	alias(libs.plugins.androidApplication) apply false
}

val androidEnabled = providers
	.gradleProperty("enableAndroid")
	.map(String::toBoolean)
	.orElse(true)

if (androidEnabled.get()) {
	pluginManager.apply(libs.plugins.androidApplication.get().pluginId)
	extensions.configure<ApplicationExtension> {
		namespace = "de.lehrbaum.firefly"
		compileSdk = libs.versions.android.compileSdk.get().toInt()

		defaultConfig {
			applicationId = "de.lehrbaum.firefly"
			minSdk = libs.versions.android.minSdk.get().toInt()
			targetSdk = libs.versions.android.targetSdk.get().toInt()
			versionCode = 1
			versionName = "1.0"
		}
		packaging {
			resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
		buildTypes {
			getByName("release") {
				isMinifyEnabled = false
			}
		}
		compileOptions {
			sourceCompatibility = JavaVersion.VERSION_11
			targetCompatibility = JavaVersion.VERSION_11
		}
	}
	dependencies {
		add("debugImplementation", compose.uiTooling)
	}
}

@OptIn(ExperimentalWasmDsl::class)
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

	wasmJs {
		compilerOptions {
			outputModuleName.set("app")
		}
		binaries.executable()
		browser {
			commonWebpackConfig {
				outputFileName = "app.js"
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
				implementation(compose.components.uiToolingPreview)
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
		val wasmJsMain by getting {
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
