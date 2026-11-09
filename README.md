# MediaX

MediaX is a Kotlin Multiplatform mobile scaffold with shared business logic and fully native user interfaces:

- Android: Jetpack Compose and AndroidX ViewModel
- iOS: SwiftUI and `ObservableObject`
- Shared: Kotlin domain models and repository contracts

The initial vertical slice is a media dashboard backed by deterministic in-memory data. It intentionally does not include networking, persistence, authentication, analytics, uploading, or playback.

## Requirements

- JDK 17
- Android Studio with Android SDK 37 (the app continues to target API 36)
- Supabase CLI and a Docker-compatible runtime for local backend development
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

## Supabase development setup

The Android build requires two public client values. They are injected into generated `BuildConfig` fields and are never hard-coded in Kotlin:

- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`

Only use a publishable client key. Never place a Supabase secret key or legacy `service_role` key in this repository, a local Android configuration file, CI output, or an APK. Client security depends on authentication and row-level security; a publishable key is not a replacement for either.

### Local Supabase stack

1. Install the [Supabase CLI](https://supabase.com/docs/guides/local-development) and start a Docker-compatible runtime.
2. From the repository root, start the checked-in local project and inspect its generated connection values:

   ```sh
   supabase start
   supabase status --env
   ```

3. Copy `supabase.properties.example` to the ignored `supabase.properties` file.
4. Set `SUPABASE_PUBLISHABLE_KEY` to the `PUBLISHABLE_KEY` reported by `supabase status --env`.
5. Set `SUPABASE_URL` to the reported API URL. An Android emulator reaches the development host through `10.0.2.2`, so replace the reported loopback host while preserving its API port. For example, `http://127.0.0.1:54321` becomes `http://10.0.2.2:54321`.

A physical device needs a host address it can reach on the development network. Do not expose the local Supabase stack to the public internet. Local seeds and development records must contain fake household data only.

### Hosted development project or CI

For a hosted environment, use a dedicated development Supabase project's URL and publishable key from its Connect dialog. Do not use the production household project or production financial data during development.

CI can supply the same names as environment variables; environment variables take precedence over `supabase.properties`:

```sh
SUPABASE_URL=https://example-development.supabase.co \
SUPABASE_PUBLISHABLE_KEY=sb_publishable_replace_me \
./gradlew :androidApp:assembleDebug
```

Android build and test tasks fail before compilation with an actionable message when either value is absent, the URL is malformed, or a secret/service-role key is supplied. The configuration values are not printed by validation.

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

