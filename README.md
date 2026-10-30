# MediaX

MediaX is a Kotlin Multiplatform mobile scaffold with shared business logic and fully native user interfaces:

- Android: Jetpack Compose and AndroidX ViewModel
- iOS: SwiftUI and `ObservableObject`
- Shared: Kotlin domain models and repository contracts

The initial vertical slice is a media dashboard backed by deterministic in-memory data. It intentionally does not include networking, persistence, authentication, analytics, uploading, or playback.

## Requirements

- JDK 17
- Android Studio with Android SDK 37 (the app continues to target API 36)
- Xcode 26 or newer
- An iOS 16+ simulator

Gradle is provided through the checked-in wrapper; no global Gradle installation is required.

## Project structure

| Module | Responsibility |
| --- | --- |
| `sharedLogic` | Domain models, the `MediaRepository` contract, and deterministic sample implementation |
| `androidApp` | Android entry point, native ViewModel, Material 3 Compose dashboard, and Android tests |
| `iosApp` | Xcode project, native SwiftUI dashboard, ViewModel, unit tests, and UI smoke test |

Platform ViewModels own loading and presentation state. They receive the shared repository through constructor injection, leaving both UI stacks easy to test and ready for a future remote repository.

## Run Android

1. Open the repository root in Android Studio.
2. Allow Gradle sync to finish.
3. Select the `androidApp` run configuration and an API 26+ device.

From the command line:

```sh
./gradlew :androidApp:assembleDebug
```

## Run iOS

Open `iosApp/MediaX.xcodeproj`, choose the shared `MediaX` scheme and an iOS 16+ simulator, then Run. The project's first build phase invokes Gradle to create and link the `SharedLogic` framework automatically.

Command-line simulator build:

```sh
xcodebuild \
  -project iosApp/MediaX.xcodeproj \
  -scheme MediaX \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  build
```

## Verify

Run Kotlin formatting checks, shared tests, Android unit tests, lint, and assembly:

```sh
./gradlew ktlintCheck :sharedLogic:allTests :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug
```

Run Android UI tests with an emulator connected:

```sh
./gradlew :androidApp:connectedDebugAndroidTest
```

Run iOS unit and UI tests with an available simulator:

```sh
xcodebuild \
  -project iosApp/MediaX.xcodeproj \
  -scheme MediaX \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  test
```

