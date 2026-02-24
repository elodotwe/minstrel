# Minstrel Agent Guidelines

This document provides guidelines for AI agents working on the Minstrel codebase.

## 1. Build, Lint, and Test Commands

### Build

To build the entire project, run:

```bash
./gradlew build
```

To assemble the debug APK, run:

```bash
./gradlew assembleDebug
```

### Lint

There is no specific linter configured in this project. Adhere to the existing code style.

### Tests

This project has two types of tests: unit tests and instrumented tests.

**Run all unit tests:**

```bash
./gradlew testDebugUnitTest
```

**Run a single unit test:**

Use the `--tests` flag to specify the test class and method.

```bash
./gradlew testDebugUnitTest --tests "com.jacobarau.minstrel.ExampleUnitTest.addition_isCorrect"
```

**Run all instrumented tests:**

These tests run on an Android device or emulator.

```bash
./gradlew connectedDebugAndroidTest
```

**Run a single instrumented test:**

```bash
./gradlew connectedDebugAndroidTest -P android.testInstrumentationRunnerArguments.class=com.jacobarau.minstrel.ExampleInstrumentedTest#useAppContext
```

## 2. Code Style Guidelines

### Formatting

- **Indentation:** Use 4 spaces for indentation.
- **Line Length:** Keep lines under 120 characters.
- **Whitespace:** Use standard Kotlin conventions for whitespace around operators and after commas.

### Imports

- **Order:** While no strict ordering is enforced, group imports by package. For example, `android`, `androidx`, `com`, `kotlin`, `kotlinx`, `java`.
- **Wildcards:** Do not use wildcard imports.

### Types

- Always use explicit type annotations for function return types and public properties.
- Use type inference for local variables where the type is obvious.

### Naming Conventions

- **Classes and Interfaces:** `PascalCase`. Examples: `MainActivity`, `TrackRepository`.
- **Functions:** `camelCase`. Examples: `onCreate`, `getTracks`.
- **Variables and Properties:** `camelCase`. Examples: `trackListState`, `playbackState`.
- **Composables:** `PascalCase`. Examples: `TrackList`, `PlayerControls`.
- **`StateFlow` properties:** Name them as nouns that describe the state they hold. Examples: `val tracks: StateFlow<TrackListState>`, `val playbackState: StateFlow<PlaybackState>`.

### Error Handling

- For operations that can fail, use sealed classes to represent the state (e.g., `Loading`, `Success`, `Error`). `TrackListState` is a good example of this pattern.
- For permission checks, update the state to reflect that permissions are missing.
- Avoid using `try-catch` blocks for expected error conditions. Instead, model them as part of the state.

### Architecture

- **MVVM:** The project follows the Model-View-ViewModel (MVVM) architecture pattern.
  - **View:** Jetpack Compose functions in the `com.jacobarau.minstrel.ui` package.
  - **ViewModel:** `TrackViewModel` is responsible for holding and processing UI-related data.
  - **Model:** The `repository` and `data` packages contain the data and business logic.
- **Dependency Injection:** Hilt is used for dependency injection. When adding new dependencies, provide them in the appropriate Hilt module (e.g., `PlayerModule`, `RepositoryModule`).
- **Asynchronous Programming:** Use Kotlin Coroutines and Flows for all asynchronous operations.
  - `StateFlow` is used to expose state from ViewModels.
  - Repositories expose data using `Flow`.

### Jetpack Compose

- **State Hoisting:** Hoist state to the lowest common ancestor. ViewModels should be the source of truth for screen state.
- **Recomposition:** Be mindful of performance. Avoid unnecessary recompositions by passing stable types to Composables.
- **Previews:** Create `@Preview` Composables for UI components to facilitate development and testing. `MainActivity.kt` has good examples of this.

## 3. General Principles

- **Read Existing Code:** Before adding new features or fixing bugs, read the surrounding code to understand the existing patterns and conventions.
- **Keep it Simple:** Write simple, readable, and maintainable code.
- **Add Tests:** For new features, add corresponding unit or instrumented tests.
- **No Third-Party Libraries:** Do not add new third-party libraries without a compelling reason. The existing stack (Hilt, Coroutines, Flow, Jetpack Compose, Media3) should be sufficient for most tasks.
