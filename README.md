# Widgeter

**Your home screen, upgraded.** A fast, private, beautifully designed Android
app with **21 home-screen widgets** and a full in-app companion — no accounts,
no internet, no tracking. Everything stays on your device.

![Aqua theme · Material 3 · 21 widgets](https://img.shields.io/badge/widgets-21-1E88E5) ![minSdk 26](https://img.shields.io/badge/Android-8.0%2B-2FCB6E)

---

## The widgets

| | Widget | What it does |
| --- | --- | --- |
| ⏰ | **Clock** | Time, date and a greeting; compacts when resized small |
| 🕰️ | **Analog clock** | A classic ticking clock with a theme-aware face |
| 🌍 | **World clock** | A second time zone — tap to cycle through cities |
| 📝 | **Note** | A named note; tap to edit. Add as many as you like |
| 🔢 | **Counter** | A named counter with **+**/**−** and a custom step |
| ✅ | **To-do** | A checklist with drag-to-reorder, priorities and due dates |
| 🔥 | **Habit** | A daily check-in that tracks your streak |
| 💧 | **Water** | Log glasses of water; resets each day |
| 🎯 | **Countdown** | Days until (or since) a date you choose |
| ⏱️ | **Stopwatch** | A self-ticking stopwatch you can start from the home screen |
| ⏲️ | **Timer** | A countdown timer — presets **or any custom length** |
| 🍅 | **Pomodoro** | Focus/break cycles with a session counter (lengths configurable) |
| 📅 | **Calendar** | A month view with today highlighted |
| 🗓️ | **Day info** | Weekday, ISO week number, day-of-year and days left |
| 📊 | **Progress** | Percent of the year / month / week / day elapsed |
| 🌙 | **Moon** | Tonight's moon phase and illumination, computed offline |
| 🔋 | **Battery** | Battery level with a bar and charging status |
| 💬 | **Quote** | A quote of the day — tap to shuffle |
| 🎲 | **Random** | Coin flip, dice or a 0–100 number — tap to roll |
| 💰 | **Savings goal** | Progress toward a target with **+**/**−** and a bar |
| ⚡ | **Quick actions** | Shortcut buttons: new note, task or counter |

Every widget is **resizable** (1×1 up to full screen) and **theme-aware**.

## Designed to look good

- **Aqua** default theme — a blue → teal → green gradient, plus **Red, Blue,
  Yellow and Black** accents you can switch in Settings.
- **Custom typography** — [Sora](https://fonts.google.com/specimen/Sora) for
  display and [Manrope](https://fonts.google.com/specimen/Manrope) for body,
  so nothing looks like the stock system font.
- **Material 3** surfaces with rounded, elevated cards and a floating bottom nav.
- **Real background blur on Android 12+** — dialogs and pickers frost the
  screen behind them (gracefully plain on older versions).
- **Light & dark** themes, with Material You dynamic color where available.

## In-app companion

Tabbed pages for **Notes · Counters · Tasks · Clock · More** let you add,
rename, recolor, reorder and delete items — and every home-screen widget can
show any item you create. The **More** page holds habits, water trackers and
countdowns; **Clock** has stopwatch, a custom-length timer and alarms;
**Settings** has appearance, accent color, Pomodoro lengths, and
**backup / restore** of all your data as a single JSON file.

---

## Get it onto your phone (no dev tools needed)

Every build publishes a versioned GitHub **Release** with the APK attached.

1. Open the latest release: **https://github.com/Rc7676/widgeter/releases/latest**
2. Under **Assets**, tap **`widgeter-<version>.apk`** to download (do this on
   the phone for the least friction).
3. Open the file and tap **Install**. The first time, Android asks you to
   **allow installing from this source** — expected for apps installed outside
   the Play Store. Accept, then install.
4. Long-press an empty spot on your home screen → **Widgets** → find
   **Widgeter** → drag any widget onto your screen. (Or browse them in-app:
   **More → Browse widgets**.)

> This is a **debug**-signed build — perfect for personal use and sideloading.
> It installs as an update over previous versions and keeps your data.

## Build it yourself (optional)

Open the project in **Android Studio** and press **Run**, or from a machine
with the Android SDK + JDK 17:

```bash
gradle assembleDebug
# APK lands in app/build/outputs/apk/debug/app-debug.apk
```

## Tech notes

- Kotlin, classic `AppWidgetProvider` + `RemoteViews` (lightweight, no Compose).
- `minSdk 26` (Android 8.0+), `targetSdk 34`; Material 3 (`Theme.Material3`).
- State stored in `SharedPreferences`; collections serialized as JSON.
- Self-ticking `Chronometer` / `TextClock` widgets; `AlarmManager`-backed
  alarms and timer with boot reschedule.
- Real blur via `windowBlurBehindEnabled` (API 31+), scoped with a `values-v31`
  resource qualifier.
- **No internet permission, no analytics, no accounts** — all data stays
  on-device.
