# Peptide Tracker

An offline-first Android app for personal peptide inventory and recordkeeping. The existing legacy Java logbook remains available; the peptide inventory and dosing workflow is implemented as a separate Kotlin + Compose entry screen backed by a local Room database.

## Peptide tracking

- Unopened stock: peptide, vendor, lot, milligrams per vial, vial count, price, storage location, and optional expiration
- Reconstitution: atomically consumes one unopened vial and creates an active vial with calculated concentration
- Active vials: remaining-volume indicator and fresh / expiring / expired status
- Dose logger: U-100 or U-40 unit calculation, site rotation suggestion, saved dose history, and atomic vial-volume decrement
- Calculator: standalone syringe-unit scratchpad with a visual tick scale
- Inventory, active vials, and dose history persist locally in Room without an account or network

## Legacy logbook

- Inventory: peptide name, quantity, unit, storage notes, expiration, and price paid
- Dose history: name, amount, unit, date, and notes
- Supplies: item, quantity, and notes
- Daily schedule: recurring planned items with time, amount, unit, and notes

Legacy records and the schedule are saved locally on the device with Android app preferences. The schedule is a personal planning list and does not send reminders. This app is a recordkeeping aid only; it does not provide medical, dosing, mixing, or storage advice. Follow product labeling and guidance from a qualified healthcare professional.

## Build

Open this folder in Android Studio and allow Gradle sync, then run the `app` configuration on an emulator or Android device. The project uses the Android Gradle plugin and Android SDK; this workspace does not include a Gradle wrapper. The app configuration includes Room via KSP, Compose Material 3, Lifecycle ViewModel, coroutines, and WorkManager dependencies.
