# U5 Pro Cinematic Car Launcher

A free, experimental native Android launcher for the U5 Pro / TS18 landscape head unit (1280 × 720). No root required.

## Features
- Dark blue cinematic dashboard with GPS speed display and clock
- Navigation opens the installed maps app
- Music media keys (play/pause, previous, next)
- App drawer and shortcuts for phone, music, radio, and Android Settings
- Separate launcher app; factory software is not modified

**Limitations:** The design is an original, functional first version, not a pixel-perfect implementation of the concept image. Map is a shortcut, not an embedded live map. Vehicle CAN bus, reversing camera, fuel level, factory radio controls, Bluetooth integration, weather, and media metadata are *not* implemented. The speed display needs permission and a GPS fix; never rely on it as a vehicle speedometer.

## Build (free GitHub Actions)
1. Open the **Actions** tab.
2. Select **Build U5 Pro Launcher APK**. If no run appears, click **Run workflow**.
3. Open the latest run and ensure the Build APK step is green.
4. Under **Artifacts**, download **U5Pro-Cinematic-Launcher-APK**.
5. Extract the downloaded ZIP to find `app-debug.apk`. Install that APK on the head unit while parked.

## Installation / recovery
- Test by opening the launcher from the app drawer **before** making it default.
- If prompted for a Home app, choose **Just once** until tested.
- Keep the original factory launcher installed. To switch back, go to Settings > Apps > Default apps > Home app, if your firmware supports that menu.
- Do not change firmware, root, disable factory apps or modify reverse camera settings.
- Android 13 is reported by the head unit but firmware compatibility is not yet verified.

Built with Java and the Android SDK; no paid services or API keys required.
