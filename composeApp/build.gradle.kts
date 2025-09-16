import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidApplication)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.serialization)
	alias(libs.plugins.buildKonfig)
}

kotlin {
	androidTarget {
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_11)
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
		androidMain.dependencies {
			implementation(compose.preview)
			implementation(libs.androidx.activity.compose)
			implementation(libs.ktor.client.cio)
		}
		commonMain.dependencies {
			implementation(compose.runtime)
			implementation(compose.foundation)
			implementation(compose.material3)
			implementation(compose.ui)
			implementation(compose.components.resources)
			implementation(compose.components.uiToolingPreview)
			implementation(libs.androidx.lifecycle.viewmodelCompose)
			implementation(libs.androidx.lifecycle.runtimeCompose)
			implementation(libs.ktor.client.core)
			implementation(libs.ktor.client.contentNegotiation)
			implementation(libs.ktor.serialization.kotlinxJson)
			implementation(libs.kotlinx.datetime)
			implementation(libs.napier)
		}
		iosArm64Main.dependencies {
			implementation(libs.ktor.client.darwin)
		}
		iosSimulatorArm64Main.dependencies {
			implementation(libs.ktor.client.darwin)
		}
		commonTest.dependencies {
			implementation(libs.kotlin.test)
			implementation(libs.ktor.client.mock)
			implementation(libs.kotlinx.coroutines.test)
			@OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
			implementation(compose.uiTest)
		}
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

android {
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
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
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
	debugImplementation(compose.uiTooling)
}
