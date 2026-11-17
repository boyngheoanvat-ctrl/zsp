# ERI Battle V2

Completely new Android package `com.eri.battlebot` — not a patch of the old Xmagic/ERI bot. No old license, library, background bot process, troop routines, or native code included.

## Features
- Pixel-based Battle detection from an **actual Android screenshot** obtained via AccessibilityService.takeScreenshot (Android 11+).
- Multiscale-safe normalized search region; yellow/orange connected components with geometric confidence.
- Calculates actual tap coordinates from the detection center, never fixed positions.
- Confirms location across two screenshots, 4.5-second cooldown, and refuses taps unless Clash of Clans is the foreground app.
- Entirely offline and no background telemetry.

## Scope
This app only presses the **Battle** button. It intentionally does not recreate the previous attack cycle, troop deployment, account switching or any other previous bot features.

## Build
Android SDK 35 + JDK 17 + Gradle 8.9. Run `gradle :app:assembleDebug` inside this directory or use the GitHub Actions workflow in this branch.

The detector was checked on the user's provided game screenshot, where a large button-shaped connected yellow component was found in the expected bottom-left region. It has **not** been validated by real-device installation yet.
