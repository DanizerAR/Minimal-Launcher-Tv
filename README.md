# CleanYourTV — Minimal TV Home Launcher

A minimal, lightweight home screen launcher for Android TV, built for the OnePlus TV and other Android TV devices. Clean, fast, and stripped down to the essentials: a pinned dock, an app grid, a clock, and Bing wallpaper support.

## Origin

This repository is a fork of **[Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv)**, customized for a minimal OnePlus home experience. Credit to Gathin23 for the original launcher code.

## Current version

**v3.13** (versionCode 22)

- 7 bundled icons at 480×270 JPEG (icon-03, 08, 09, 10, 11, 14, 15)
- Bundled Outfit font removed; system fonts used instead (~111 KB saved)
- Bing wallpaper of the day hard-coded to the **India market** (`mkt=en-IN`)
- APK size: ~637 KB

## Features

- tvOS-style home screen: black background, pinned dock row, app grid, glass focus highlight
- App grid with reordering (hold OK on a tile)
- App hide/show from the launcher grid
- Bing photo of the day as wallpaper (India market)
- Wallpaper picker with dim slider
- Clock screensaver with 12/24-hour format
- Festival skins (Diya, etc.) and F1 race calendar support
- Launcher settings activity (columns, rows, dock opacity, glow, clock size, etc.)
- Open-on-start choice: boot directly into a chosen TV app
- minSdk 26 / targetSdk 34 / compileSdk 34

## Repository structure

```
launcher/
  app/src/main/
    java/com/gathin/tvhome/   # launcher source
    res/                      # resources, layouts, values
    assets/icons/             # bundled icons (JPEG, 480x270)
```

## Building

This is a standard Android Gradle project. Build the release APK:

```bash
cd launcher
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 ./gradlew :app:assembleRelease \
  --offline --no-daemon --max-workers=2 \
  -Dorg.gradle.java.home=/usr/lib/jvm/java-17-openjdk-arm64 \
  -Dorg.gradle.jvmargs=-Xmx1500m \
  -Pandroid.aapt2FromMavenOverride=/usr/lib/android-sdk/build-tools/34.0.0/aapt2
```

The output APK is at `launcher/app/build/outputs/apk/release/app-release.apk`.

## License

Inherited from the original [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) project. Please check the upstream repository for license details.

## Attribution

- **Original project:** [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv)
- **This fork:** [DanizerAR/Minimal-Launcher-Tv](https://github.com/DanizerAR/Minimal-Launcher-Tv)
