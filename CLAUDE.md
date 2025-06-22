# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Japanese Vertical Text Viewer Android application designed as a companion app for the Jota+ text editor. It displays Japanese text in traditional vertical format with ruby notation (furigana) support.

## Build Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build on device/emulator
./gradlew installDebug

# Run lint checks
./gradlew lint
./gradlew lintDebug

# Run tests (note: no tests currently implemented)
./gradlew test
```

## Architecture

The project is a multi-module Android application:

### Modules
- **app/** - Main application module (jp.gr.aqua.jota.vtextviewer)
  - Contains only LaunchActivity.kt for launching the viewer
  - Version 1.20, targets Android API 36

- **vtextview/** - Library module with vertical text rendering
  - **jp.gr.aqua.vjap** - Core vertical text rendering engine
    - VTextView.kt - Main vertical text view widget
    - VTextLayout.kt - Layout engine for vertical text
    - VerticalLayout.kt - Custom layout manager
    - CharPoint.kt, VChar.kt - Character positioning and rendering
    - Rubys.kt - Ruby notation support
  - **jp.gr.aqua.vtextviewer** - Application functionality
    - MainActivity.kt - Main viewer activity
    - RubyToHtmlConverter.kt - Converts ruby notation to HTML
    - PreferenceActivity.kt/Fragment.kt - Settings UI

### Key Technical Details
- Based on vjap library (https://github.com/taizan/vjap) for vertical text rendering
- Uses WebView for HTML rendering with vertical CSS
- Supports multiple ruby notation formats for Japanese text
- Includes bundled IPA Gothic/Mincho and BizUD fonts
- Can receive text via Android share intent from other apps

### Build Configuration
- Kotlin 2.1.10, Android Gradle Plugin 8.9.3
- Min SDK 23 (Android 6.0), Target SDK 36 (Android 16)
- ProGuard enabled for release builds
- View Binding enabled for both modules

## Development Notes

When modifying the vertical text rendering:
- The core logic is in VTextLayout.kt which calculates character positions
- Ruby support is handled by Rubys.kt and RubyToHtmlConverter.kt
- The app can run standalone (shows settings) or receive shared text from Jota+
- Font assets are in vtextview/src/main/assets/