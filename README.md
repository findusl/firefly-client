# Firefly Client

Firefly Client is a Kotlin Multiplatform client and a small collection of API
utilities for [Firefly III](https://www.firefly-iii.org/). This is a personal,
non-commercial project for a private target group. It is not an official
Firefly III client.

The shared Compose UI and business logic target Android, iOS, desktop JVM, and
WebAssembly. The currently supported mobile baselines are Android 14 (API 34)
and iOS 18.

## Project layout

- `composeApp/src/commonMain` contains shared UI and business logic.
- `composeApp/src/*Main` contains platform-specific implementations.
- `androidApp` packages the shared code as the Android application.
- `iosApp` contains the SwiftUI iOS application and Xcode project.
- `openapi_helper.py` explores the vendored Firefly III OpenAPI specification.

## Prerequisites

- JDK 21
- Android SDK 37 for Android builds
- Xcode 26.4 or newer for iOS builds (the app targets iOS 18)
- Python 3.13 for the OpenAPI helper

## Build and test

Run the baseline formatting and shared JVM tests:

```shell
./gradlew checkAgentsEnvironment --parallel --console=plain
```

Build the Android debug application:

```shell
./gradlew :androidApp:assembleDebug
```

Run the desktop application or start the Wasm development server:

```shell
./gradlew :composeApp:run
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

For iOS, open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme.
The app can also be built without signing from the command line:

```shell
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

## Firefly III connection

Set `BASE_URL` and `ACCESS_TOKEN` before building or running the client. Do not
commit either value; local environment files are ignored.

```shell
export BASE_URL=https://firefly.example
export ACCESS_TOKEN=your-personal-access-token
```

## OpenAPI helper

The repository vendors the Firefly III 6.6.6 OpenAPI specification. Install the
helper dependencies in a virtual environment:

```shell
python3.13 -m venv .venv
.venv/bin/python -m pip install --requirement requirements.txt
```

List endpoints, inspect one endpoint, or make a request to a configured demo
environment:

```shell
.venv/bin/python openapi_helper.py
.venv/bin/python openapi_helper.py /v1/accounts
.venv/bin/python openapi_helper.py /v1/accounts --request
```

The helper censors `BASE_URL` and `ACCESS_TOKEN` in responses and request-error
messages.
