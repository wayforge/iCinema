# Repository Guidelines

## Project Structure & Module Organization

iCinema is a single-module Android app. The Gradle module is `:app`, with source under `app/src/main/java/com/icinema`.

- `data/`: API clients, Room database/DAO/entity code, DTOs, mappers, and repository implementations.
- `domain/`: domain models shared by UI and data layers.
- `pages/`: feature UI and MVI-style screen logic, grouped by feature such as `home`, `detail`, `player`, `category`, `favorite`, and `history`.
- `di/`: Hilt modules and dependency wiring.
- `ui/theme/`, `util/`, and `pages/widgets/`: shared theme, helpers, and reusable Compose widgets.
- `app/src/main/res`: Android resources and launcher assets.
- `app/src/test` and `app/src/androidTest`: local JVM tests and instrumented Android tests.
- `app/schemas`: Room schema exports; update these when migrations change.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper:

- `./gradlew assembleDebug`: build a debug APK.
- `./gradlew test`: run local JVM unit tests.
- `./gradlew connectedAndroidTest`: run instrumented tests on a connected device or emulator.
- `./gradlew lint`: run Android lint. Lint currently checks dependencies and does not abort the build on errors.
- `./gradlew :app:compileDebugKotlin`: quick Kotlin compile check for app changes.

## Device / Emulator Rules

- 禁止擅自启动本地 AVD / emulator（含 `emulator -avd`、后台拉起模拟器等），除非用户明确要求。
- `adb install`、真机/模拟器调试、instrumented 测试前先 `adb devices`；若无 `device`，只提示用户连接设备或自行启动模拟器，然后停止，不代为启动 AVD。
- 仅在用户明确要求时才启动、关闭或重启模拟器。

## Coding Style & Naming Conventions

Code is Kotlin with Java 17 toolchains and Jetpack Compose. Use 4-space indentation, idiomatic Kotlin, and clear names. Keep packages aligned with the existing feature and layer layout. Name Compose functions in `PascalCase`, ViewModels as `FeatureViewModel`, reducers as `FeatureReducer`, contracts as `FeatureContract`, and Hilt modules as `FeatureBindingsModule` or `*Module`. Prefer immutable UI state and StateFlow-based updates.

## Testing Guidelines

Use JUnit for local tests and AndroidX/JUnit/Espresso/Compose test libraries for instrumented tests. Put fast logic tests in `app/src/test/java` and device/UI tests in `app/src/androidTest/java`. Name tests after the unit under test, for example `HomeReducerTest` or `PlayerViewModelTest`, and cover reducers, mappers, repositories, and Room migrations when behavior changes.

## Commit & Pull Request Guidelines

Recent commits use short, lower-case summaries such as `fix`, `home`, and `ref player`; keep messages concise but more descriptive when possible, for example `fix player preload state`. Pull requests should include a brief problem/solution summary, test commands run, linked issues when relevant, and screenshots or screen recordings for visible Compose UI changes.

## Configuration & Architecture Notes

Dependencies are centralized in `gradle/libs.versions.toml`. The app uses Hilt, Room with KSP schema export, Retrofit/OkHttp, Coil, Media3 ExoPlayer, and Compose Material/Material3. When changing Room entities or migrations, keep `app/schemas` current and verify migration behavior.
