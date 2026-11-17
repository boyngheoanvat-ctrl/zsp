# ERI COC V4 — In-game menu (Android)

A new standalone Android AccessibilityService app styled like a compact ImGui overlay. It is **not** a fork of the original Xmagic bot, and it does not modify Clash of Clans binaries or processes.

## Features actually implemented

- **Six menu tabs:** Chính, Đấu, Lính, Làng, Hội, Log. Compact draggable "✦ ERI" bubble and draggable dark panel.
- **Offline OCR-assisted, one-action navigation:** After a user presses a menu shortcut, hide the overlay; capture a fresh game screenshot, find the requested on-screen button text, scale its centre to the actual display size and tap **once**. Report a clear error if the button is not visible. No fixed fallback tap and no game memory access.
- Navigation shortcuts: Battle, Edit Army, Battle Log, Build/Upgrade, Collect (visible "Collect" buttons only), Laboratory, Heroes, Equipment/Blacksmith, Pets, Shop, Builder Base, Profile, Clan, Clan Wars, Donate (opens visible donate button only), Clan Capital, Raids and Events.
- **Battle automation (experimental, inherited from V3):** image-heuristic Battle identification; configurable troop-slot count and drop count; opt-in auto-deploy when OCR recognizes battle HUD; manual in-game deploy button; optional OCR Return Home.
- **Diagnostics:** screen-text OCR preview, recent local event log, detected coordinates and phase.
- **No auto-spending** gems, loot, builder time, raid currency, no confirmation button press for buying/upgrading or donations.
- **Privacy:** All screenshots and OCR run on device. No uploads or tracking.
- Android 11+, no root.

## Not implemented (avoid overpromising)

This is **not** a fully autonomous Clash of Clans player. It cannot guarantee smart base search, scoring, troop strategy, 3-star wins, accurate upgrade selection, magic-item management, war participation, donation, capital attack or account automation. Menu shortcut buttons navigate only when the corresponding game label is visible in the current screenshot. The user must open nested screens and confirm resource-consuming actions. This protects against unintended spending and version-specific UI changes.

Supercell changed the game UI in October 2026, moving Edit Army and Battle Log into Battle and centralizing upgrades under Build; use the new in-game UI accordingly.

## Android implementation

Package `com.eri.battlebot`, version 4.0. It uses AccessibilityService screenshot capture, gesture dispatch and bundled on-device Google ML Kit text recognition. No game injection or root access.

Build on GitHub Actions from branch `eri-coc-v4-menu` (`eri-battle-v2/gradle` project). The workflow artifact contains a complete Android APK.

**Testing warning:** CI can confirm successful compilation and package signing; only a device session can confirm that game-specific OCR targets and troop deployment work.
