# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application for wireless ADB control that allows connecting to another Android device via wireless ADB to install APKs, list apps, and uninstall applications. Built with Kotlin and Jetpack Compose, it uses the Dadb library for ADB protocol implementation to maintain reliability while keeping the app size under 15MB.

## Build Commands

```bash
# Build debug version
./gradlew assembleDebug

# Build release version with ProGuard minification
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug

# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run Android instrumented tests
./gradlew connectedAndroidTest
```

## Architecture

### Core Components

- **DadbClient Wrapper** (`app/src/main/java/com/adbtool/adb/DadbClient.kt`): Wrapper implementation around Dadb library providing singleton pattern and unified API
- **Command Layer** (`app/src/main/java/com/adbtool/commands/`): Encapsulates ADB commands for install, uninstall, and app listing operations using DadbClient
- **MVVM Architecture**: ViewModels manage state and business logic, Compose UI observes StateFlow for reactive updates
- **Connection Management**: DadbClient singleton manages single device connection with automatic cleanup

### Dadb Library Integration

- **Dependency**: `dev.mobile:dadb:1.2.10` - Third-party ADB implementation library
- **Benefits**: More reliable connection handling, better error recovery, comprehensive ADB protocol support
- **Integration Pattern**: Wrapper pattern maintains existing architecture while leveraging Dadb internally
- **Connection**: Automatic connection validation, proper resource cleanup, and error handling

### Key Data Models

- `ConnectionState`: Tracks connection status (DISCONNECTED/CONNECTING/CONNECTED/ERROR) and device address
- `AppInfo`: Contains package name, app name, version info for third-party applications
- `TransferProgress`: Real-time file transfer metrics (bytes, speed, estimated time remaining)

### Technical Stack

- **Language**: Kotlin 1.9.20
- **UI**: Jetpack Compose with Material3 (BOM 2023.10.01)
- **Async**: Coroutines with StateFlow for state management
- **ADB**: Dadb library (`dev.mobile:dadb:1.2.10`) for ADB protocol implementation
- **Build**: Gradle 8.5 with Kotlin DSL, configuration cache enabled
- **Target**: Android 8.0+ (API 26-34)
- **APK Size**: ~14MB (Release build with ProGuard)

## Core Features

1. **Wireless ADB Connection**: Connect via IP:port (default 5555), supports legacy Android versions without pairing
2. **APK Installation**: Direct APK installation using Dadb library with progress tracking and error handling
3. **App Management**: List third-party apps (`pm list packages -3`), get details (`dumpsys package`), uninstall with confirmation
4. **Real-time Progress**: Transfer progress shows percentage, speed, and estimated time remaining
5. **Enhanced Reliability**: Robust connection management through Dadb library with automatic reconnection attempts

## Key Implementation Details

- **File Transfer**: Uses Dadb library's built-in file transfer capabilities with optimized streaming
- **Connection Management**: DadbClient singleton manages single device connection with automatic cleanup and validation
- **Error Handling**: Uses Kotlin Result type for operation results with proper error propagation and recovery
- **Migration Benefits**: More reliable ADB operations, better device compatibility, comprehensive error handling
- **No Git Operations**: This project follows a strict rule where Claude does not execute any git commands

## Important File Locations

- Main entry: `app/src/main/java/com/adbtool/MainActivity.kt`
- ADB client wrapper: `app/src/main/java/com/adbtool/adb/DadbClient.kt`
- Command implementations: `app/src/main/java/com/adbtool/commands/`
- Connection screen: `app/src/main/java/com/adbtool/ui/screens/ConnectScreen.kt`
- App list screen: `app/src/main/java/com/adbtool/ui/screens/AppListScreen.kt`
- Build configuration: `app/build.gradle.kts`

## Development Guidelines

- Follow Kotlin official coding conventions
- Use Material3 design principles for UI
- All network/IO operations must use coroutines
- Maintain MVVM separation with StateFlow for reactive updates
- Keep dependencies minimal to maintain small app size
- Use DadbClient wrapper for all ADB operations - do not use Dadb directly in UI/ViewModel layer
- Test reset: Use reflection-based singleton reset in tests to ensure test isolation
- Error handling: Always handle Result types properly and propagate errors appropriately

### Utility Classes

- **Constants**: `app/src/main/java/com/adbtool/utils/Constants.kt` - Centralized constants for default ports, IP formats, and configuration values
- **SafeExecution**: `app/src/main/java/com/adbtool/utils/SafeExecution.kt` - Unified error handling utility with Result extension functions
- **FormatUtils**: `app/src/main/java/com/adbtool/utils/FormatUtils.kt` - Utility functions for formatting file sizes and time durations
- **CacheUtils**: `app/src/main/java/com/adbtool/utils/CacheUtils.kt` - Cache management utilities
- **DebugLog**: `app/src/main/java/com/adbtool/utils/DebugLog.kt` - Centralized logging with consistent formatting

### UI Components

- **CommonComponents**: `app/src/main/java/com/adbtool/ui/components/CommonComponents.kt` - Reusable UI components including LoadingButton, LoadingOutlinedButton, SectionTitle, and common shapes
- **TransferProgressDialog**: Progress display for file transfers with real-time metrics
- **UninstallConfirmDialog**: Confirmation dialog for app uninstallation
- **LogViewerDialog**: Dialog for viewing debug logs

## Code Quality Standards

- All error handling should use SafeExecution utility for consistency
- UI components should leverage CommonComponents for reusable elements
- Log messages should follow format: "Description - Status Symbol" (e.g., "Connection established - ✓")
- Constants must be used instead of hardcoded values
- Maintain backward compatibility when adding new features

## Recent Improvements

- Migrated from custom ADB implementation to Dadb library for better reliability
- Added unified error handling through SafeExecution utility
- Extracted common UI components to reduce code duplication
- Centralized constants management
- Standardized logging format with status symbols
- Optimized for APK size < 5MB with ProGuard minification