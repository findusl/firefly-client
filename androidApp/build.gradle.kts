import org.gradle.api.JavaVersion

plugins {
	alias(libs.plugins.androidApplication)
	alias(libs.plugins.composeCompiler)
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

	buildFeatures {
		compose = true
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
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}

dependencies {
	implementation(project(":composeApp"))
	implementation(libs.androidx.activity.compose)
	implementation(libs.compose.ui.tooling.preview)
	debugImplementation(libs.compose.ui.tooling)
}
