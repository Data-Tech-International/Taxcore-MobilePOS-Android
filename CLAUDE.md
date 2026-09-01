# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TaxCore Mobile POS — an Android point-of-sale app for small/medium businesses. Handles invoice creation, catalog management, journal tracking, and tax compliance via the TaxCore API.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew test                   # Run unit tests
./gradlew clean                  # Clean build
./gradlew printVersionInformation # Print version info
```

## Tech Stack

- **Kotlin 2.1.0**, JDK 17, Compile/Target SDK 36, Min SDK 24
- **Single module** (`:app`), Groovy-based `app/build.gradle`
- **View Binding** (XML layouts, no Compose)
- **Realm 10.19.0** for local database (schema v3, UI-thread access enabled)
- **Retrofit 3.0.0** + Gson for networking with custom certificate pinning
- **Dagger 2.21** for dependency injection (KAPT)
- **Firebase Crashlytics** for crash reporting
- **EventBus** for inter-component communication
- **EncryptedSharedPreferences** for sensitive config storage

## Architecture

R
**MVP-lite pattern** with Dagger 2 DI:

- Activities extend `BaseActivity` (implements `HasSupportFragmentInjector`)
- Fragments contain presentation logic directly (no separate Presenter classes)
- `AppComponent` → `AppModule` + `ActivityBuilder` → `MainActivityModule` (binds fragments)

**Navigation flow**: `SplashActivity` → `DashboardActivity` → feature screens (Invoice, Catalog, Journal, Settings)

**Data layer**:

- `data/realm/` — Realm model classes (Item, Journal, Cashier, Taxes, Cert)
- `data/local/` — Manager classes for database operations (CatalogManager, InvoiceManager, JournalManager, TaxesManager, CertManager)
- `data/api/` — Retrofit API service and client with certificate handling
- `data/models/` — API response DTOs
- `data/params/` — API request objects
- `PrefService` — encrypted preferences wrapper

**Key API endpoints** (v3):

- `POST api/v3/invoices` — submit invoices
- `GET api/v3/status` — fetch taxes/status
- `GET api/v3/environment-parameters` — configuration

## Global App State

`AppSession` object holds runtime state: `isAppConfigured`, `shouldAskForConfiguration`, `pinCode`, `pacCode`.

## Versioning

Version defined in `app/build.gradle`: `versionMajor * 10000 + versionMinor * 100 + versionPatch * 10 + appVersionBuild`. Current: 3.5.1.

## Localization

Multi-language: French (fr), Serbian (sr), Bosnian (bs). Dynamic locale switching via `CtxUtils.updateLocale()` with context wrapper.

## CI/CD

GitHub Actions (`.github/workflows/v3-release.yml`) builds on pushes to `main-v3`/`purs` branches and PRs. Uploads to Firebase App Distribution.

## Skills

- `create-spec` — Create a feature specification at `docs/specs/<feature_name>-spec.md` with requirements interview and structured template

## Conventions

- Commit messages in present tense describing what the commit does to the code
- Debug keystore at `keys/taxcore-debug.jks` (credentials in `gradle.properties`)
- Release signing injected via CI/CD from GitHub Secrets
