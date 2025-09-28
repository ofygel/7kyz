# Android Client Module

This module contains the Android client implementation for the Freedom app. It
is configured for Android 6.0 (API 23) as both the minimum and target SDK.

## Building locally

1. Install a compatible version of Gradle (8.2 or newer) if you do not already
   have it on your machine.
2. From the `androidApp` directory, generate the wrapper JAR that is omitted
   from the repository:

   ```bash
   gradle wrapper
   ```

   The command recreates `gradle/wrapper/gradle-wrapper.jar` so the provided
   `./gradlew` script can bootstrap the build without requiring a global Gradle
   installation afterwards.
3. Run the desired tasks, for example:

   ```bash
   ./gradlew :app:assembleDebug
   ```

## Notes

- The Gradle wrapper JAR is intentionally excluded to keep the repository free
  of binary artifacts.
- Compose Material 3 is used for the UI; ensure you are running with a recent
  version of Android Studio (Giraffe or newer) for full IDE support.
