# Project Plan

Native Android application for Android Auto with motorcycle turn-by-turn navigation tracking, Google Auth for user authentication, Firebase Realtime Database / Firestore backend for real-time live location sharing among users, built with Jetpack Compose and Material 3 design libraries.

## Project Brief

# Project Brief: Motorcycle Turn-by-Turn Navigation & Live Location Sharing

## Features
- **Google Authentication**: Secure single-tap user authentication using Google Auth / Credential Manager to establish rider profiles and session state.
- **Motorcycle Turn-by-Turn Navigation & Android Auto Integration**: Clear visual and voice turn-by-turn routing with native support for Android Auto heads-up display integration.
- **Real-Time Group Location Sharing**: Live tracking and display of nearby ride group participants on an interactive map backed by Firebase Realtime Database / Firestore.
- **Adaptive Ride Dashboard**: High-contrast, glanceable telemetry interface showcasing current speed, distance to next turn, ETA, and connected riders.

## High-Level Tech Stack
- **Language**: Kotlin
- **UI & Design**: Jetpack Compose, Material 3
- **Navigation & Adaptive Strategy**: **Jetpack Navigation 3** (state-driven) and **Compose Material Adaptive** library for responsive layouts across phone and Android Auto form factors
- **Core Architecture & Async**: Kotlin Coroutines, StateFlow, ViewModel, Lifecycle
- **Location & Auto Services**: Fused Location Provider API, Maps SDK for Android, Android Auto App Library (`androidx.car.app`)
- **Backend & Services**: Google Auth (Credential Manager API), Firebase Realtime Database / Firestore for real-time location sync

## Implementation Steps

### Task_1_SetupAuthAndDataModels: Configure Firebase, Google Auth with Credential Manager API, core data models (Rider, Group, Location, Telemetry), and repository layer for Firebase location sync.
- **Status:** COMPLETED
- **Updates:** Firebase & Credential Manager dependencies added, data models created, FirebaseAuthRepository and Firebase repositories for live location and group session sync created and verified with unit tests and assembleDebug.
- **Acceptance Criteria:**
  - Firebase & Credential Manager dependencies and configuration set up
  - Data models for Rider location, group session, and navigation telemetry defined
  - Google Auth integration implemented via Credential Manager
  - Firebase Realtime Database repository implemented for location streaming

### Task_2_NavigationAndLocationEngine: Implement FusedLocationProvider service, Maps SDK interactive map with route plotting, and turn-by-turn navigation telemetry calculation.
- **Status:** COMPLETED
- **Updates:** Maps SDK, FusedLocationProvider Foreground Service, TurnByTurnEngine telemetry calculation, and NavigationMapScreen Compose UI built and verified with unit tests and assembleDebug.
- **Acceptance Criteria:**
  - API_KEY integration for Google Maps SDK configured and functional
  - FusedLocationProvider tracking current location with foreground service
  - Maps SDK interactive map displaying route overlays and turn maneuver markers
  - Navigation engine calculating speed, ETA, and distance to next maneuver

### Task_3_RideDashboardAndGroupSharingUI: Build Jetpack Compose UI featuring high-contrast Ride Dashboard, turn-by-turn guidance banner, and real-time group rider map overlay.
- **Status:** COMPLETED
- **Updates:** High-contrast Ride Dashboard, real-time group rider map overlay, group session creation/join flows, and Nav 3 flow completed and verified with unit tests.
- **Acceptance Criteria:**
  - High-contrast Jetpack Compose Ride Dashboard displaying speed, ETA, and upcoming turn
  - Real-time location markers for nearby ride group members updated on map
  - Group session creation and join flow integrated
  - Jetpack Navigation 3 flow for Auth, Navigation Dashboard, and Group Sharing

### Task_4_AndroidAutoIntegration: Implement Android Auto support using androidx.car.app library with CarAppService, Session, and heads-up navigation CarScreen.
- **Status:** COMPLETED
- **Updates:** Android Auto CarAppService, MotorcycleCarSession, and MotorcycleCarScreen with NavigationTemplate, PaneTemplate, and real-time state sync implemented and verified.
- **Acceptance Criteria:**
  - Android Auto CarAppService and Navigation CarScreen declared and configured
  - Glanceable turn-by-turn navigation and group telemetry rendered on Android Auto display
  - Data sync between main app state and Android Auto CarScreen working seamlessly

### Task_5_RunAndVerify: Execute full application build and instruct critic_agent to verify application stability, map rendering, Android Auto support, and group location sync.
- **Status:** COMPLETED
- **Updates:** App built, installed, launched, and verified on connected device by critic_agent. Auth, Ride Dashboard, Turn-by-Turn banner, Maps SDK, Group sharing simulator, and Android Auto integration checked with 0 crashes.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - critic_agent verifies application stability (no crashes), confirms alignment with user requirements, and reports critical UI issues

### Task_6_PlaceSearchAndStarredLocations: Implement floating Google Maps style place search bar, Places API / Geocoder helper, SavedPlacesRepository for bookmark management, Location Details bottom sheet with scrollable photo carousel, and Starred Places management UI.
- **Status:** COMPLETED
- **Updates:** Floating Google Maps style place search bar, Location Details bottom sheet with 10 scrollable photo items, Navigate action, Star toggle button, SavedPlacesRepository, and Starred Places management sheet implemented and verified with unit tests.
- **Acceptance Criteria:**
  - API_KEY integration for Google Places API configured and functional
  - Floating place search bar with query input and auto-suggestions dropdown integrated on main map screen
  - Location Details bottom sheet displaying details, 10 scrollable photo items, Navigate action, and Star toggle button
  - SavedPlacesRepository persisting starred places locally with reactive Flow updates
  - Starred Places bottom sheet listing bookmarked places with Navigate and Remove actions

### Task_7_RunAndVerify: Execute full application build and instruct critic_agent to verify application stability, place search bar auto-suggestions, location details photo carousel, navigation route plotting, and starred places management.
- **Status:** COMPLETED
- **Updates:** Task_7_RunAndVerify verified on emulator with zero crashes. Place search, photo carousel, route navigation, and starred places verified working.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - critic_agent verifies application stability (no crashes), confirms alignment with user requirements, and reports critical UI issues
- **Duration:** N/A

