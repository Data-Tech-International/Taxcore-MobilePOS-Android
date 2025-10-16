# TaxCore mobile POS

# Overview

TaxCore mobile POS is an Android POS app for any small or medium-sized business. It offers the end
user a friendly interface that incorporates touch input while interfacing with a minimum set of POS
functions.

TaxCore mobile POS enables taxpayers to easily keep track of their invoices and product lists, as
well as not to worry about being compliant with the current tax regulations system.

## Disclaimer

> TaxCore mobile POS is a free app, available for commercial use, therefore we are not responsible for
> the contents of the imported/exported files, and we are not responsible for any lost data, corrupted
> or damaged imported/exported files, so always make a backup.
> We do not take responsibility for decisions taken and data entered by the users of TaxCore mobile
> POS, based solely on the information provided in this app.

## Prerequisites

Before you can start using the TaxCore mobile POS app, make sure you have met several requirements:

-   Functioning Android handheld device (phone or tablet) running on Android 7.0
-   Available internet connectivity
-   Functional built-in camera (optional)
-   An active account on Taxpayer Administration Portal

# Using the app

For detailed instructions on how to install and use TaxCore mobile POS, see [official user manual](https://github.com/Data-Tech-International/Taxcore-MobilePOS-Android/wiki)

## Technology Stack

-   **Language**: Kotlin 2.1.0
-   **Min SDK**: Android 7.0 (API 24)
-   **Target SDK**: Android 36
-   **Architecture**: MVP with Dagger 2 dependency injection
-   **Database**: Realm 10.19.0
-   **Networking**: Retrofit 2.9.0

# Contributing

The main purpose of this repository is to continue evolving TaxCore mobile POS, making it better and easier to use. Development of TaxCore mobile POS happens in the open on GitHub, and we are grateful to the community for contributing bugfixes and improvements. Read below to learn how you can take part in improving this app.

## Contributing Prerequisites

-   Android Studio
-   Android SDK 36
-   Gradle 8.13
-   JDK 17

## Building the Project

```bash
# Clone the repository
git clone https://github.com/Data-Tech-International/Taxcore-MobilePOS-Android.git

# Build debug variant
./gradlew assembleDebug

# Run tests
./gradlew test

# Clean build
./gradlew clean
```

## How to contribute

If you want to contribute to a project and make it better, your help is very welcome.

If you are looking to make your first contribution, follow the steps below.

-   Fork this repository
-   Create an issue
-   Create a branch
-   Make necessary changes and commit those changes
-   Push changes to GitHub
-   Submit your changes for review via PR

And last but not least: Always write your commit messages in the present tense. Your commit message should describe what the commit, when applied, does to the code – not what you did to the code.

## License

TaxCore mobile POS is [MIT licensed](./LICENSE).
