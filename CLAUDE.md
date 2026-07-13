# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

"Aurora Launcher" (`rootProject.name = "Aurora Launcher"`, `applicationId com.aurora.game`) is an Android client for a GTA: San Andreas multiplayer server. It bundles:

- A launcher UI (nickname entry, server ping, asset download/install) written in Java.
- The GTA:SA mobile game engine (the WardrumStudios/NVIDIA "devtech" Android port layer) bridged over JNI.
- A native C++ SA-MP-style multiplayer client (RakNet networking, game hooks via RenderWare, ImGui-based mobile UI, voice chat) compiled as `libmultiplayer.so`.

Game assets (models/textures/audio) are **not** bundled in the APK; they are downloaded at first run from a Hugging Face dataset repo declared in `app/src/main/assets/update.json`.

## Build

Standard Gradle/Android build, driven by `gradlew` (Gradle 9.0-milestone-1, AGP 8.2.1, compileSdk/targetSdk 34, minSdk 26, NDK 26.2.11394342).

```
./gradlew assembleVer_debugDebug      # debug flavor, debug build type
./gradlew assembleVer_releaseRelease  # release flavor, release build type
./gradlew assembleDebug               # all flavors, debug build type
./gradlew installVer_debugDebug       # build + install on connected device/emulator
```

There is a single `flavorDimensions "version"` with two product flavors — `ver_debug` and `ver_release` — crossed with the normal `debug`/`release` build types, so four variants exist in total (`ver_debugDebug`, `ver_debugRelease`, `ver_releaseDebug`, `ver_releaseRelease`). Both flavors currently share the same `aurora.jks` signing config. Output APKs are renamed to `Aurora-<buildType>.apk`.

`BuildConfig.IS_DEBUG` and `BuildConfig.FLAVOR_NAME` are generated per-flavor and used to branch debug-only behavior at runtime.

### Native (C++) build

The native side is built via CMake (`app/src/main/cpp/CMakeLists.txt`), invoked automatically by AGP's `externalNativeBuild`. ABIs are restricted to `armeabi-v7a` and `arm64-v8a` (`ndk.abiFilters`). Three CMake subprojects are combined:

- `opus/` — vendored Opus audio codec (voice chat), built as a static lib with `-O3`.
- `multiplayer/` — the actual game/network client. `CMakeLists.txt` glob-includes essentially the whole `multiplayer/` tree (`game/`, `gui/`, `net/`, `util/`, `voice_new/`, `vendor/`) up to ~10 directory levels deep — **new `.cpp`/`.c` files anywhere under `multiplayer/` are picked up automatically**, no need to register them in CMake.
- Depends on `shadowhook` (function hooking library, resolved via `prefab`) and the prebuilt `libbass.so` (audio) per-ABI under `multiplayer/vendor/bass/libs/<ABI>/`.

There is no separate command to build native code in isolation; it's driven by the Gradle build (`externalNativeBuildDebug`/`externalNativeBuildRelease` tasks if needed standalone).

### Tests

Only the default Android templates are present (`androidx.test`/JUnit4/Espresso deps in `app/build.gradle`); no meaningful test suite exists yet. Standard commands would be `./gradlew testVer_debugDebugUnitTest` (JVM unit tests) and `./gradlew connectedVer_debugDebugAndroidTest` (instrumented), but treat these as scaffolding, not a safety net.

## Architecture

### Java/Kotlin layer (`app/src/main/java`)

- **`com.aurora.launcher`** — the launcher app shell, independent of the native game engine:
  - `GoldParaha` (`Application` subclass, wired in the manifest) — global crash handler (kills the process on uncaught exceptions) and Firebase init.
  - `activities/HomeActivity` — entry point (`LAUNCHER` intent). Loads/saves the player nickname and server host/port to `<externalFilesDir>/SAMP/settings.ini` (INI via `org.ini4j`), pings the SA-MP server for live player count (`util/SampQueryAPI`), and routes to either `LoadGameActivity` (assets missing/outdated) or straight into `com.aurora.game.SAMP` (the native activity).
  - `activities/LoadGameActivity` — first-run/update installer. Reads `assets/update.json` for a Hugging Face `repo_id`/`revision`/`files_path_prefix`, walks the HF `tree` API (paginated via `Link: rel="next"` headers) to enumerate remote files under that prefix, downloads any that are missing/size-mismatched into `getExternalFilesDir()`, then writes a local `version.txt`. This local `version.txt` (currently pinned to `"2"`) is the sole "is data installed / up to date" signal used by both this activity and `HomeActivity.isDataUpdateAvailable()` — **the two checks are duplicated and must be kept in sync** if the version scheme changes.
  - `activities/InstallDataActivity` — an older/alternate install-flow stub (simulated progress, no real download); not wired into the manifest's active flow.
  - `activities/LoadApkActivity` — empty stub, declared in the manifest but unimplemented.
  - `util/ConfigValidator` — seeds `SAMP/settings.ini` from the bundled asset on first run.
  - `util/SignatureChecker` — verifies the installed APK's signing certificate SHA-256 against a hardcoded expected value (tamper/repackaging check).
  - `util/SampQueryAPI` — raw SA-MP UDP query protocol client for server info (Codeobfuscated naming, e.g. `mo7164b()`).
  - `update/UpdateManager` / `UpdateChecker` / `UpdateInfo` — a second, more general Hugging-Face-backed update pipeline (overlaps conceptually with `LoadGameActivity`'s inline logic).

- **`com.aurora.game`** — the native-engine-facing layer:
  - `GTASA` (extends `com.wardrumstudios.utils.WarMedia`, the base Android game-activity engine wrapper) — loads native libraries in a fixed order (`ImmEmulatorJ` optional, then `GTASA`, `bass`, `multiplayer`), initializes `ShadowHook`, and exposes the native `main()` entry point.
  - `SAMP` (extends `GTASA`) — the actual game `Activity` launched from `HomeActivity`; wires up in-game UI (`ui/CustomKeyboard`, `ui/dialog/DialogManager`, `ui/AttachEdit`, `ui/LoadingScreen`, `ui/Hud`, `ui/tab/*`) to native calls (e.g. `native void sendDialogResponse(...)`). Most of the interesting logic here is the Java side of a JNI bridge — expect matching native handlers under `cpp/multiplayer/`.
  - `HeightProvider` — keyboard/IME height plumbing, mirrored in `com.nvidia.devtech.HeightProvider`.

- **`com.nvidia.devtech`** and **`com.wardrumstudios.utils`** — vendored base engine code from the original GTA:SA Android port (APK asset loading, media/billing/gamepad helpers). Treat as third-party/engine code; avoid restructuring it without reason.

### Native layer (`app/src/main/cpp`)

- `multiplayer/game/` — SA-MP-equivalent game logic split into `Core`, `Entity`, `Render`, `RW` (RenderWare engine interop), `Collision`, `Animation`, `Tasks`, `Widgets`, `Mobile/` (mobile-specific menu/settings), `Pipelines`, `Plugins`, `Textures`, `Events`, `Enums`.
- `multiplayer/gui/` — ImGui-based (`vendor/imgui`) in-game UI plus `samp_widgets`.
- `multiplayer/net/` — networking on top of vendored RakNet (`vendor/raknet`).
- `multiplayer/voice_new/` — voice chat, built on Opus.
- `multiplayer/vendor/` — third-party dependencies vendored in-tree: RakNet, Opus (static lib consumed here), imgui, armhook, SimpleIni, quaternion, str_obfuscator, bass prebuilt `.so`s per-ABI.

Java↔native calls are linked by name: native methods declared `native` in `SAMP`/`GTASA` are implemented somewhere in `multiplayer/`; when changing a native method signature, grep the C++ tree for the JNI-mangled or registered name rather than assuming a single obvious file.

## Notable conventions / gotchas

- Several classes are annotated `@Obfuscate` (`com.joom.paranoid`) — these are intentionally obfuscated in release builds (e.g. `SignatureChecker`, `SAMP`, `GTASA`). Don't rely on their bytecode/reflection identity surviving in release.
- Locale: user-facing strings in the launcher activities are a mix of Portuguese (`LoadGameActivity`, `InstallDataActivity`) and Russian (progress text in `LoadGameActivity.initViews`) — this reflects real product copy, not placeholder text; don't "fix" the language without checking with the requester.
- The signing keystore path/password/alias are committed in `app/build.gradle` (`aurora.jks` / `aurora228`) and shared by both flavors — treat this repo as already having that secret exposed; don't add new secrets the same way.
- **The hardcoded storePassword/keyPassword (`aurora228`) does not actually match `app/aurora.jks`.** Any `release`-build-type packaging task (`packageVer_*Release`) fails with `KeytoolException: keystore password was incorrect`. `debug`-build-type packaging works regardless, because AGP always signs the `debug` build type with the auto-generated Android debug keystore, ignoring the flavor's `signingConfig`. CI (`.github/workflows/android-build.yml`) currently only builds `assembleDebug` because of this — don't re-enable release assembly/signing until the correct password is supplied.
- Networking uses `usesCleartextTraffic="true"` and a custom `network_security_config` — the SA-MP server and Hugging Face downloads are plain HTTP/UDP in places, this is expected for this app, not an oversight to silently "fix".
- `version.txt` under `getExternalFilesDir()` is the single source of truth for "is game data installed/current"; several places read/write it independently (`HomeActivity`, `LoadGameActivity`, `InstallDataActivity`) — when changing the versioning scheme, update all of them.
