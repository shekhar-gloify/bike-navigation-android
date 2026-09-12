# Motorcycle Turn-by-Turn Navigation & Live Location Sharing

**Bike Navigation Android** is a high-contrast, sunlight-visible native Android application built specifically for motorcycle riders. Featuring a high-visibility UI designed for outdoor readability, turn-by-turn guidance, real-time group tracking, place search, location photos, starred places, and Android Auto integration.

---

## Overview

Designed for outdoor conditions and glove-friendly interaction, Bike Navigation Android provides a comprehensive motorcycle navigation and group riding experience. Built using modern Android architecture, Jetpack Compose, Material 3, and Google Maps SDK, the application delivers smooth map interactions, real-time telemetry overlays, interactive place details with photo carousels, and seamless Android Auto display capabilities.

---

## Key Features

- **Dashboard HUD**: High-contrast instrument cluster showing current speed, heading, speed limit indicator, compass overlay, weather condition summaries, and quick toggle controls tailored for outdoor motorcycle visibility.
- **Turn-by-Turn Guidance**: Audio-visual maneuver instructions, distance-to-turn indicators, remaining ETA/distance calculations, maneuver icons, and route recalculation prompts.
- **Place Search & Discovery**: Real-time autocomplete search for places, gas stations, scenic viewpoints, restaurants, and custom coordinates with direct route previewing.
- **Location Details & Photo Carousel**: Interactive bottom sheet presenting place descriptions, contact info, ratings, operation hours, and photo carousels.
- **Starred Locations**: Save, organize, filter, and quick-navigate to favorite locations or saved riding waypoints.
- **Local Group Simulator & Live Location Sharing**: Create or join group riding sessions with real-time location sharing, rider markers on map, distance-to-leader calculations, and live telemetry updates.
- **Android Auto Integration**: Native `androidx.car.app` support providing projected turn-by-turn navigation screens, trip summaries, and quick action controls directly on motorcycle dashboard displays and Android Auto head units.

---

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material Design 3](https://m3.material.io/)
- **Maps & Location**: [Google Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk), Google Places API, Fused Location Provider (`com.google.android.gms:play-services-location`)
- **Automotive Display**: Android for Cars App Library (`androidx.car.app:car`)
- **Architecture & Async**: MVVM architecture, Kotlin Coroutines, `StateFlow`, `SharedFlow`, Android Jetpack ViewModel & Lifecycle components

---

## Getting Started & Setup

### Prerequisites

- **Android Studio**: Ladybug / 2024.2.1 or newer
- **JDK**: Java 17 or higher
- **Android SDK**: API Level 34 (Android 14) or higher
- **Google Maps API Key**: A valid API key with **Maps SDK for Android** and **Places API** enabled.

### Installation & Configuration

1. **Clone the repository**:
   ```bash
   git clone https://github.com/shekhar09/bike-navigation-android.git
   cd bike-navigation-android
   ```

2. **Configure API Keys**:
   Create or edit the `local.properties` file in the project root directory and add your Google Maps API key:
   ```properties
   MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY_HERE
   ```

3. **Build the Project**:
   Build the debug APK using the Gradle wrapper:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run Unit Tests**:
   Execute unit test suites to verify business logic, repositories, turn-by-turn navigation engine, and view models:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## Android Auto Testing

Bike Navigation Android includes native Android Auto templates via `androidx.car.app`. To test the automotive interface on your development workstation:

1. **Install Desktop Head Unit (DHU)**:
   - In Android Studio, open **SDK Manager** > **SDK Tools**.
   - Select and install **Android Auto Desktop Head Unit emulator**.

2. **Connect Device / Emulator**:
   - Enable Developer Options and USB Debugging on your Android device or start an Android Emulator.
   - Start the Android Auto companion app on your phone / emulator and enable **Developer Settings** > **Start Head Unit Server**.

3. **Forward Port & Launch DHU**:
   - Run the ADB port forwarding command:
     ```bash
     adb forward tcp:5277 tcp:5277
     ```
   - Launch the DHU binary from your Android SDK location:
     ```bash
     $ANDROID_SDK_ROOT/extras/google/desktop-head-unit/desktop-head-unit
     ```
   - Select **Bike Navigation** from the Android Auto app drawer.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
