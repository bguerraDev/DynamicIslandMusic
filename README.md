# DynamicIslandMusic

[![Official Website](https://img.shields.io/badge/Website-musicislandapp.com-00DC82?style=flat&logo=googlechrome&logoColor=white)](https://musicislandapp.com)
[![Google Play Store](https://img.shields.io/badge/Google_Play-MusicIsland-414141?style=flat&logo=googleplay&logoColor=white)](https://play.google.com/store/apps/details?id=com.bguerradev.musicisland)

[![Status](https://img.shields.io/badge/Service-Operational-brightgreen?style=flat)]()
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)]()

[![License](https://img.shields.io/badge/License-Custom_Permissive-blue.svg)]()

[Read this in Spanish](README.es.md)

An experimental **Dynamic Island-style overlay for Android**, inspired by iOS.  
The app displays a floating island with real-time playback state, soundwaves, and expand/collapse interactions while maintaining strict control over visibility rules and service lifecycles.

> **Production Notice**: The prototype and foundational research from this v1 repository have evolved into **MusicIsland (Production)**, featuring a full Clean Architecture rewrite, hardware-accelerated audio frequency visualizers, Material You theming, and zero-telemetry privacy guarantees.
>
> - **Official Website**: [https://musicislandapp.com](https://musicislandapp.com)
> - **Download on Google Play**: [MusicIsland on Google Play Store](https://play.google.com/store/apps/details?id=com.bguerradev.musicisland)

---

## Features

- Overlay pill with **collapsed** and **expanded** states
- **Media playback awareness** (play, pause, stop) via `MediaNotificationListener` + `MediaSessionsBus`
- Auto-hide rules: screen off, inactivity timeout, target app in foreground
- Foreground service orchestration with persistent notification
- Soundwave animation (`Lottie`) running when playing / frozen when paused
- Expand mode (`IslandExpandedActivity`) with optional **blur background** (Android 12+)
- **Settings screen** with instant toggle (Hilt-injected `SettingsViewModel`)

---

## Visual Comparison & Screenshots

### DynamicIslandMusic v1 (Experimental Prototype)

<p float="left">
  <img src="screenshots/image_1.png" width="45%" alt="DynamicIslandMusic v1 Collapsed State" />
  <img src="screenshots/image_2.png" width="45%" alt="DynamicIslandMusic v1 Expanded State" />
</p>

### MusicIsland Production Version (Available on Google Play)

The screenshots below illustrate the refined production version available on the Play Store, featuring native notch anchoring, hardware-accelerated FFT visualizers, and customized theme engines:

<p float="left">
  <img src="screenshots/musicisland_prod_home.png" width="45%" alt="MusicIsland Prod - Status Bar Notch Alignment" />
  <img src="screenshots/musicisland_prod_expanded.png" width="45%" alt="MusicIsland Prod - Expanded Media Island with Background Blur" />
</p>

<p float="left">
  <img src="screenshots/musicisland_prod_visualizer.png" width="45%" alt="MusicIsland Prod - Real-Time FFT Audio Visualizer" />
  <img src="screenshots/musicisland_prod_theme.png" width="45%" alt="MusicIsland Prod - Dynamic Pill Themes Engine" />
</p>

---

## Architecture

The project V1 follows a **CLEAN-lite MVVM** approach with **modern Android best practices**:

- **App layer**
  - `DynamicIslandApp.kt` with `@HiltAndroidApp`
  - `DynamicIslandTheme.kt` (Material 3 + Compose)

- **Domain layer**
  - Use cases (`ControlPlaybackUseCase`, `GetPlaybackStateUseCase`, `ShowIslandUseCase`, `HideIslandUseCase`, `IsTargetAppInForegroundUseCase`)

- **Data layer**
  - `SettingsRepository`, `UsageStatsRepository` (with caching + throttled logging)
  - Media bus for session updates

- **Presentation layer**
  - Jetpack Compose UI (`IslandRoot`, `IslandOverlay`, `MusicPopUp`, `SoundWaveLottie`)
  - `SettingsScreen`
  - ViewModels (with Hilt DI)

- **Services**
  - `MediaNotificationListener` (attaches `MediaController`)
  - `IslandForegroundService` (orchestrates state machine + overlay)

- **Overlay**
  - `IslandStateMachine` (centralized visibility rules + timers)
  - `OverlayWindowManager` (`TYPE_APPLICATION_OVERLAY` pill/expanded window)

---

## Tech Stack

- **Kotlin + Jetpack Compose + Material 3**
- **Hilt** for dependency injection
- **ViewModel + StateFlow**
- **ForegroundService + Notification API**
- **WindowManager.TYPE_APPLICATION_OVERLAY**
- **Lottie animations**
- **UsageStatsManager** for target app detection

---

## Project Structure

```plaintext
app/
 └── src/main/java/com/bryanguerra/dynamicislandmusic/
     ├── app/              # Application & Theme
     ├── data/             # Repositories & MediaSessionBus
     ├── di/               # Hilt Modules
     ├── domain/           # UseCases
     ├── overlay/          # StateMachine & OverlayManager
     ├── services/         # Foreground service & Notification listener
     ├── ui/               # Compose UI
     ├── util/             # Helpers & Constants
     └── viewmodel/        # ViewModels
```

---

## License

This project is licensed under a **custom permissive license**:

- You are free to **clone, modify and experiment** with the code.
- **Attribution is required**: any redistribution, fork, or derived project must **credit Bryan Guerra (@bguerraDev)** clearly in README or About screens.
- The original idea and concept remain property of Bryan Guerra.

``` text
Copyright (c) 2026 Bryan Guerra (@bguerraDev)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to use,
copy, modify, and experiment with the Software, subject to the following conditions:

1. Attribution must be given to the original author:
   Bryan Guerra (GitHub: @bguerraDev)
2. Redistributions, forks, or derivative works must include this notice.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
```

---

## Roadmap & Production Evolution

- [x] Initial experimental prototype for Android Dynamic Island overlay
- [x] Lottie soundwave synchronization with active media sessions
- [x] Background blur exploration on Android 12+
- [x] **Full Clean Architecture Evolution (Completed in MusicIsland Production)**:
  - Transitioned from experimental prototype to a modular enterprise architecture.
  - Official deployment to Google Play Store and dedicated ecosystem landing page:
    - **Website**: [musicislandapp.com](https://musicislandapp.com)
    - **Google Play**: [Download MusicIsland](https://play.google.com/store/apps/details?id=com.bguerradev.musicisland)

---

## Author

Developed by **Bryan Guerra ([@bguerraDev](https://github.com/bguerraDev))**