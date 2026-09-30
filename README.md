# Since Us ❤

A privacy-friendly Android app for couples: counts how long you have been together and reminds you of every special day. Available in English and German.

<p>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" width="200" alt="Overview">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" width="200" alt="Live counter">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg" width="200" alt="Setup">
</p>

## Features

- Days, weeks, months and hours together, plus a live counter down to the second
- Your own photo with adjustable framing, or one of several backgrounds
- Notifications on monthly and yearly anniversaries, every 100 days, fun numbers like 222 and every 50 weeks, at a time you choose
- Upcoming special days with a countdown
- Two home screen widgets: your photo with your days together, and a compact counter in Material You colors
- Guided setup, optional start time, one-tap "delete all data"
- Light and dark mode, English and German (per-app language supported)
- No account, no ads, no tracking, all data stays on the device

## Download

| Where | Updates |
| --- | --- |
| [GitHub Releases](https://github.com/DerDieDasLian/loveapp/releases) | The app checks GitHub once a day and notifies you (can be turned off). Also works with [Obtainium](https://github.com/ImranR98/Obtainium). |
| F-Droid | Through F-Droid (planned) |
| Google Play | Through the Play Store (planned) |

The three versions are signed differently, so switching between them requires uninstalling first.

Tip: if notifications arrive late, set *Settings → Apps → Since Us → Battery* to "Unrestricted".

## Build variants

| Variant | Gradle task | Internet | Updates |
| --- | --- | --- | --- |
| `github` | `./gradlew assembleGithubRelease` | only for the update check | GitHub Releases |
| `fdroid` | `./gradlew assembleFdroidRelease` | none | F-Droid |
| `play` | `./gradlew bundlePlayRelease` | none | Google Play |

Tests, screenshots and store graphics: `./gradlew testGithubDebugUnitTest` (output in `app/build/screenshots` and `app/build/store`).

Release signing and publishing are described in [RELEASING.md](RELEASING.md) (German).

## Tech

Kotlin, Jetpack Compose (Material 3), Glance widgets, DataStore, Coil. Minimum Android 8.0.

## How this app was made

This app was developed with AI assistance: the code was written with the help of an AI coding assistant and reviewed, tested and directed by the maintainer.

## License

[GNU General Public License v3.0 or later](LICENSE)
