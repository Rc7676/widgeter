# Widgeter

A Material 3 Android app with **seven home-screen widgets**:

| Widget | What it does |
| --- | --- |
| ⏰ **Clock** | Time, date, and a greeting; compacts when resized small |
| 📝 **Note** | A named note; tap to edit. Add several, each independent |
| 🔢 **Counter** | A named counter with **+**/**−** and a custom step |
| ✅ **To-do** | A checklist with drag-to-reorder, priorities and due dates |
| 🔥 **Habit** | A daily check-in that tracks your streak |
| 💧 **Water** | Log glasses of water; resets each day |
| 🎯 **Countdown** | Days until a date you choose |

The app has a Material 3 UI with dark mode and dynamic (Material You) color,
a settings screen, per-widget setup for counters/notes/countdowns, and an
in-app screen for your primary note, counter and the shared to-do list.

---

## Get it onto your phone (no dev tools needed)

Every build publishes a versioned GitHub **Release** with the APK attached.

1. Open the latest release: **https://github.com/Rc7676/widgeter/releases/latest**
2. Under **Assets**, tap **`widgeter-<version>.apk`** to download it (do this on
   the phone for the least friction).
3. Open the downloaded file and tap **Install**. Android will ask you to
   **allow installing from this source** the first time — that's expected for
   apps installed outside the Play Store. Accept it, then install.
4. Long-press an empty spot on your home screen → **Widgets** → find
   **Widgeter** → drag any widget onto your screen.

> This is a **debug**-signed build — perfect for personal use and sideloading.
> It installs as an update over previous versions and keeps your data.

---

## Build it yourself (optional)

Open the project in **Android Studio** (it will set up Gradle for you) and press
**Run**, or from a machine with the Android SDK + JDK 17:

```bash
gradle assembleDebug
# APK lands in app/build/outputs/apk/debug/app-debug.apk
```

## Tech notes

- Kotlin, classic `AppWidgetProvider` + `RemoteViews` (lightweight, no Compose).
- `minSdk 26` (Android 8.0+), `targetSdk 34`.
- State stored in `SharedPreferences`; to-do list serialized as JSON.
- No internet permission, no analytics, no accounts — all data stays on-device.
