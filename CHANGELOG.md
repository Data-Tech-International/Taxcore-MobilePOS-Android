# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- **TBD** Invoice catalog search
- **TBD** TaxRates valid from date

### Fixed

- **TBD** Configuration response error handling

## [3.5.1] - 2025-12-16

### Added

- Added progress dialogs to journal import/export operations
- Secret export and clear journal option (5-second long press on export button)
- Added search filter to item selection bottom sheet
- Added drag handle and clear button to item search dialog

### Fixed

- Fixed recovery for corrupted EncryptedSharedPreferences
- Fixed `OutOfMemoryError` when loading large journal lists by implementing lazy loading
- Fixed `OutOfMemoryError` during journal export by implementing streaming JSON writer

## [3.5.0] - 2025-10-16

### Added

- Support for Android 16+ (API 36)
- View Binding support for all activities and fragments

### Changed

- Enhanced file management utilities for Android scoped storage
- Improved PDF creation utilities
- Updated dependencies to latest versions
- Migrated from Kotlin synthetic extensions to View Binding
- Updated Gradle to version 8.13
- Updated Kotlin to version 2.1.0
- Optimized imports across the project

## [3.4.0] - 2024-07-22

### Added

- Support for Android 14
- New language Serbian (BA) - `Serbian (Latin) (Bosnia and Herzegovina) (sr-Latn-BA)`

### Changed

- Display tax labels from all category types
- Backup disabled in manifest
- FR translations updated
- Update build gradle version
- Java compatibility version

### Fixed

- Certificate error fallback - PKCS12 key store mac invalid
- Unhandled file import/export exceptions 
- Lint warnings for deprecated syntax
- Layout issue for cashiers

## [3.3.0] - 2023-03-03

### Added

- Support for Android 10+

### Changed

- Files import/export to support Android scoped storage
- Input fields validation according to documentation specification
- Share intent to support Android 10+
- Certificate download location to cache folder
- Update security-crypto lib

### Fixed

- Language switching issue on some devices
- Storage issue on Android 10+
- Empty space after invoice in print mode
- Invoice unit price format

## [3.2.0] - 2022-12-29

### Changed

- Android minSdkVersion to 24 (Android 7)
- Gradle build version updated
- Kotlin version updated
