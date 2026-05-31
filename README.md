# Pocket Pulse

Pocket Pulse is a small Android app that quietly reminds you your phone is still with you.

## The Story

You are walking through a busy place. Maybe it is a market, a train station, a festival crowd, or a narrow lane where everyone is brushing past everyone else. Your phone is in your pocket, but after a while your brain stops checking for it. It becomes background noise.

Then comes the familiar little panic: "Is it still there?"

Pocket Pulse was made for that moment. Instead of keeping the phone silent and easy to forget, it sends a tiny vibration pattern every so often. Not a loud alarm. Not a dramatic siren. Just a private tap from your pocket that says: still here.

The timing can be randomized so you do not get too used to it. The vibration can happen as a few short bursts, so it feels intentional without being annoying. It is meant to be simple enough to start before entering a crowded area, and quiet enough to keep running while you go about your day.

Pocket Pulse cannot stop theft by itself, but it can help you notice your phone's presence before the absence becomes a surprise.

## Features

- Configurable vibration duration, burst count, and gap between bursts.
- Three interval modes:
  - Random between a minimum and maximum time.
  - Random around a chosen time with variation.
  - Exactly every chosen number of seconds.
- Foreground notification so Android knows the reminder is actively running.
- Notification stop action.
- Accent colour, icon colour, and stop button colour settings.
- Light and dark mode.
- Battery optimization warning with a shortcut to request unrestricted battery behavior.
- Small native Android build with no extra app framework.

## Package

The Android package name is:

```text
com.souravgoswami.pocketpulse
```

Android treats package names as app identities. If you previously installed an older package such as `com.sourav.pocketpulse`, uninstall that old app separately.

## Requirements

- Android Studio, or Gradle plus a configured Android SDK.
- Android SDK Platform 35 or newer.
- A physical Android phone for testing vibration. Emulators usually do not provide useful vibration feedback.
- USB debugging enabled if you want to install with `adb`.

## Build

From the project root:

```bash
./gradlew assembleRelease
```

The signed release APK will be created at:

```text
app/build/outputs/apk/release/app-release.apk
```

For a debug build:

```bash
./gradlew assembleDebug
```

The debug APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install

Install the release APK:

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

Update an existing install with the same package name and signing key:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

If the old package is still installed:

```bash
adb uninstall com.sourav.pocketpulse
```

## How To Use

1. Open Pocket Pulse.
2. Set the pulse pattern. The default is 250 ms vibration, 3 bursts, and a 50 ms gap.
3. Choose how often the reminder should happen.
4. Tap Apply Changes.
5. Tap Start Reminders before entering a crowded or risky place.
6. Keep the foreground notification enabled while the reminder is running.

On Android 13 and newer, the app asks for notification permission. On phones with aggressive battery management, use the in-app battery check and set the app to unrestricted if reminders stop when the screen is off.

## Settings

- Vibration: duration of each burst in milliseconds.
- Bursts: how many short bursts happen per reminder.
- Gap: pause between bursts in milliseconds.
- Random between: picks a fresh delay between X and Y seconds.
- Random around: picks a fresh delay around X seconds, plus or minus the variation.
- Exactly every: uses the same delay every time.
- Accent colour: changes the app header, primary buttons, and notification accent.
- Icon colour: changes the app icon tile in the header.
- Stop button colour: changes the running-state stop button.
- Dark mode: switches to a darker colour palette.

## Safety Note

Pocket Pulse is a presence reminder, not a theft-prevention guarantee. It works best as one small habit alongside the obvious things: keep your phone in a front pocket or zipped pocket, avoid leaving it exposed in a loose bag, and pause the reminder when you no longer need it.
