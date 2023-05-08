# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- **TBD** Invoice catalog search
- **TBD** TaxRates valid from date

### Fixed

- **TBD** Configuration response error handling

## [3.3.1] - 2023-05-08

### Changed

- Display tax labels from all category types
- Backup disabled in manifest
- FR translations updated

### Fixed

- Certificate error fallback - PKCS12 key store mac invalid
- Unhandled file import/export exceptions 
- Lint warnings for deprecated syntax

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
