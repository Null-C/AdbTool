# ADB Wireless Control Tool

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)

A lightweight Android application that allows you to connect to another Android device via wireless ADB to install APKs, list apps, and uninstall applications. Built with Kotlin and Jetpack Compose using the Dadb library for reliable ADB protocol implementation.

## Features

- ✅ Wireless ADB connection (supports legacy Android versions without pairing)
- ✅ APK installation with real-time progress tracking
- ✅ Third-party app listing and management
- ✅ App uninstallation with confirmation dialog
- ✅ Lightweight design (< 15MB)
- ✅ No root required
- ✅ Works in offline environments

## Screenshots

The app provides a simple and intuitive interface for wireless ADB operations:

1. **Connection Screen**: Enter device IP and connect
2. **App Management**: View installed apps and perform operations
3. **Progress Tracking**: Real-time progress for file transfers

## Requirements

- **Android Version**: 8.0+ (API 26-34)
- **Storage**: ~15MB free space
- **Network**: Wi-Fi for device connection
- **Permissions**: Internet, Network State, Storage (read for APK files)

## Installation

### From Source

```bash
# Clone the repository
git clone https://github.com/yourusername/adb-wireless-control.git
cd adb-wireless-control

# Build debug version
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug
```

### Release APK

Download the latest release APK from [Releases](https://github.com/yourusername/adb-wireless-control/releases) and install on your device.

## Usage

### 1. Connect to Device

1. Enable wireless ADB on the target device:
   ```bash
   adb tcpip 5555
   ```
2. Find the device IP address
3. Enter the IP in the app (port 5555 is default)
4. Tap "Connect"

### 2. Install APK

1. Connect to a device
2. Tap "Install APK"
3. Select the APK file (supports large files up to 200MB)
4. Monitor progress in real-time

### 3. Manage Apps

1. View all third-party installed apps
2. Tap uninstall to remove an app
3. Confirm in the dialog

## Architecture

The app follows MVVM architecture with clean separation of concerns:

```
app/
├── src/main/java/com/adbtool/
│   ├── adb/
│   │   └── DadbClient.kt          # ADB client wrapper
│   ├── commands/                  # ADB command implementations
│   │   ├── InstallCommand.kt
│   │   ├── ListAppsCommand.kt
│   │   └── UninstallCommand.kt
│   ├── data/models/               # Data models
│   │   ├── AppInfo.kt
│   │   ├── ConnectionState.kt
│   │   └── TransferProgress.kt
│   ├── ui/
│   │   ├── screens/               # UI screens
│   │   │   ├── ConnectScreen.kt
│   │   │   └── AppListScreen.kt
│   │   └── components/            # Reusable UI components
│   │       ├── CommonComponents.kt
│   │       ├── TransferProgressDialog.kt
│   │       └── UninstallConfirmDialog.kt
│   ├── utils/                     # Utility classes
│   │   ├── Constants.kt
│   │   ├── FormatUtils.kt
│   │   └── SafeExecution.kt
│   └── viewmodel/                 # ViewModels
│       ├── ConnectViewModel.kt
│       └── AppListViewModel.kt
```

## Technical Stack

| Technology | Version |
|------------|---------|
| Language | Kotlin 1.9.20 |
| UI Framework | Jetpack Compose (BOM 2023.10.01) |
| Architecture | MVVM |
| ADB Library | Dadb 1.2.10 |
| Target SDK | Android 14 (API 34) |
| Min SDK | Android 8.0 (API 26) |
| Build Tool | Gradle 8.5 |

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Setup

1. Clone the repository
2. Open in Android Studio or use the command line
3. Run tests: `./gradlew test`
4. Build the project: `./gradlew build`

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use Material 3 design principles
- Write tests for new features
- Keep PRs focused and minimal

## Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Troubleshooting

### Connection Issues

- Ensure both devices are on the same Wi-Fi network
- Check that ADB is enabled and set to TCP/IP mode on target device
- Verify firewall isn't blocking port 5555
- Try restarting the ADB daemon on target device

### Installation Issues

- Ensure sufficient storage space on target device
- Check that the APK is valid and not corrupted
- Verify "Unknown Sources" permission is enabled (for older Android versions)
- Check app install permissions in settings

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

This project uses the [Dadb](https://github.com/mobile-dev-inc/dadb) library, which is licensed under the Apache License 2.0. Please refer to the [Dadb repository](https://github.com/mobile-dev-inc/dadb) for the full terms of the Apache License 2.0.

## Acknowledgments

- [Dadb library](https://github.com/mobile-dev-inc/dadb) for ADB protocol implementation
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI framework
- [Material Design 3](https://m3.material.io/) for design guidelines

## Changelog

### v1.0.0 (Latest)
- Initial release
- Wireless ADB connection
- APK installation with progress
- App management features
- Clean and intuitive UI