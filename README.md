# Widgeter

A small Android app with **four home-screen widgets**:

| Widget | What it does |
| --- | --- |
| ⏰ **Clock** | Time, date, and a greeting that changes through the day |
| 📝 **Note** | Shows a note you edit in the app; tap the widget to open it |
| 🔢 **Counter** | A big number with **+** and **−** buttons right on the widget |
| ✅ **To-do** | A checklist you can tick off straight from the home screen |

The app itself is one simple screen where you edit the note, reset the counter,
and add/remove to-do items. Everything stays in sync with the widgets.

---

## Get it onto your phone (no dev tools needed)

The APK is built for you automatically by GitHub Actions.

1. On GitHub, open the **Actions** tab of this repo.
2. Click the most recent **“Build APK”** run (green check-mark).
3. Under **Artifacts**, download **`widgeter-debug-apk`** (a `.zip`).
4. Unzip it — you'll get **`app-debug.apk`**.
5. Send that file to your phone (email it to yourself, Google Drive, USB, etc.)
   and tap it to install. Android will ask you to **allow installing from this
   source** the first time — that's expected for apps installed outside the
   Play Store. Accept it, then install.
6. Long-press an empty spot on your home screen → **Widgets** → find
   **Widgeter** → drag any of the four widgets onto your screen.

> This is a **debug** build signed with the standard Android debug key — perfect
> for personal use and sideloading. It is not meant for the Play Store as-is.

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
