# My Voice Alarm (Android)

Native Android starter project for recording a voice message and scheduling it as an alarm.

## Features in this starter
- Record a separate voice clip for each alarm.
- Choose alarm time with Android's time picker (AM/PM shown according to phone settings).
- Daily or one-time alarms.
- Enable/disable, skip the next occurrence, and delete alarms.
- Uses Android AlarmManager and a foreground playback service; alarms can work with the screen off.
- Reschedules alarms after device reboot.

## Important setup on your phone
1. Install the APK produced by GitHub Actions.
2. Open the app and allow Microphone and Notifications.
3. Allow **Alarms & reminders / exact alarms** when Android asks.
4. In phone Settings > Apps > My Voice Alarm > Battery, choose Unrestricted (wording varies).
5. Test with an alarm 2–3 minutes ahead while the phone is locked.

## Build
The included GitHub Actions workflow builds a debug APK and uploads it as an artifact.
Open the repository's Actions tab, open the latest successful run, and download `MyVoiceAlarm-debug-apk`.

## Limitations
- Android manufacturers may delay alarms due to battery-management settings.
- Exact alarm permission and notification permission are controlled by Android.
- This starter is a first build; test on your device before relying on it for important reminders.
- Editing an existing alarm and stopping an active alarm from a dedicated in-app screen are not included in this initial starter.
