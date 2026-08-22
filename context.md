# Resell Project Context

This file is a handoff note for starting a new Codex conversation on this workspace.

## Workspace

- Workspace root: `E:\Comun\adi\Dev\Resell`
- Android project root and Git repository: `E:\Comun\adi\Dev\Resell\Resell`
- The outer workspace folder is not a Git repository.
- A convenience copy of this file may also exist at `E:\Comun\adi\Dev\Resell\context.md`; the committed copy is `E:\Comun\adi\Dev\Resell\Resell\context.md`.
- Other top-level folders:
  - `Graphics`: source/logo/image assets.
  - `Security`: keystore-related material outside the Android project.

## App

- Kotlin Android app using Jetpack Compose.
- Gradle root project name: `Resell`
- App module: `:app`
- Package / application ID: `com.resell.app`
- Launcher label comes from Android resources.
- Release APK is renamed to `TreasureMap.apk`.
- Main entry files:
  - `app/src/main/java/com/resell/app/MainActivity.kt`
  - `app/src/main/java/com/resell/app/ui/ResellApp.kt`

## Current Version And Build

- Current version in `app/build.gradle.kts`:
  - `versionCode = 18`
  - `versionName = "2.7"`
- SDK config:
  - `compileSdk = 35`
  - `minSdk = 26`
  - `targetSdk = 35`
- Release output path:
  - `app/build/outputs/apk/release/TreasureMap.apk`
- Last requested release build was for version `2.7` and succeeded.
- Release signing is configured through `keystore.properties`; do not print or expose secret values.

Useful commands, run from `E:\Comun\adi\Dev\Resell\Resell`:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

In the Codex sandbox, Gradle may fail on the user Gradle cache lock under `C:\Users\danad\.gradle\...` with `Access is denied`. If that happens, rerun the same Gradle command with escalated permissions.

## Screens And Files

- Product list page: `app/src/main/java/com/resell/app/ui/MainScreen.kt`
- Product details page: `app/src/main/java/com/resell/app/ui/DetailsScreen.kt`
- Expenses list: `app/src/main/java/com/resell/app/ui/ExpensesScreen.kt`
- Expense details: `app/src/main/java/com/resell/app/ui/ExpenseDetailsScreen.kt`
- Summary: `app/src/main/java/com/resell/app/ui/SummaryScreen.kt`
- Settings / backup: `app/src/main/java/com/resell/app/ui/SettingsScreen.kt`
- Shared UI and date picker helpers: `app/src/main/java/com/resell/app/ui/Common.kt`
- Theme/colors: `app/src/main/java/com/resell/app/ui/Theme.kt`
- Models/date helpers/filter rules: `app/src/main/java/com/resell/app/data/Models.kt`
- Persistence/backup repository: `app/src/main/java/com/resell/app/data/ProductRepository.kt`

## Data Model

- Products are `Product` objects with:
  - `description`
  - `purchasePrice`
  - `expenses`
  - `storageLocation`
  - `imageUri`
  - `deleted`
  - `platforms`
- Platform records are `PlatformListing` objects with:
  - `platform`
  - `dateListed`
  - `price`
  - `sold`
  - `dateSold`
  - `finalPrice`
- Platforms are `VINTED`, `EBAY`, `ETSY`, and `FACEBOOK`.
- Product filters are `LISTED`, `UNLISTED`, `SOLD`, `INACTIVE`, and `ALL`.
- `ALL` is intentionally last in the filter dropdown and is used as a recovery view for records that do not appear elsewhere.
- Date display format is `dd-MM-yyyy`; created-at display includes time as `dd-MM-yyyy HH:mm`.
- Data is stored in Android DataStore under preferences name `resell_products`.

## Important Product List Behavior

- Default Products filter is `ProductFilter.LISTED`.
- Default Products date range starts at `01-01-2026` and ends at today.
- Product list supports storage filtering, date filtering, search, and sort direction.
- `Listed`: not deleted, has listing info, and has no sales info.
- `Unlisted`: not deleted, no listing info, and no sales info.
- `Sold`: not deleted and has any sales signal: `sold`, `dateSold`, or `finalPrice`.
- `Inactive`: deleted records.
- `All`: every product record, including inactive/deleted and malformed or mismatched records.
- `All` bypasses date filtering, but search and storage filters still apply. Clear search/storage filters when using it as a full recovery view.
- Sold products are sorted by the latest valid `dateSold`, most recent first. Products without a parseable sold date sort to the bottom.
- Other filters use the existing created-at sort and the sort direction toggle.
- Sold date filtering checks `dateSold` values only. A product with `sold` or `finalPrice` but no valid `dateSold` can be hidden from `Sold` by date filtering; use `All` to recover it.

## Details Page Behavior

- Details page, Details box layout:
  - Description stays at the top.
  - Price/storage controls are on the left.
  - Image placeholder is on the right.
  - Active switch is on the left.
  - Take photo button is on the right.
- Product changes are draft-only until the user presses Save or answers `Yes` in the unsaved-changes dialog.
- Pressing `No` in the unsaved-changes dialog discards the draft by resetting it to the original product before leaving.
- Android back button on Product Details is handled directly by `DetailsScreen`:
  - If there are unsaved changes, show the `Save changes?` dialog.
  - If there are no changes, navigate back normally.
- Closing/killing the app while a draft is unsaved should not persist product changes because `repository.saveProduct(draft)` is only called from Save/Yes.
- Adding or taking a photo may copy an image file into app storage before Save, but the product record is not updated unless saved.

## Persistence And Backup

- `ResellApp.kt` holds navigation state and product-list filter state with `rememberSaveable`.
- Product creation time can be inferred from image file timestamps for older/legacy records.
- Backup/restore includes products, expenses, product images as base64, and Google Drive folder backup preferences.
- Auto-backup to a selected Drive folder is scheduled with WorkManager every 24 hours when enabled.

## Recent Changes

- Added `All` product filter as the last dropdown item for record recovery.
- Made `All` bypass product date filtering.
- Sorted Sold products by latest valid sale date descending.
- Made the Product Details Android back button show the unsaved-changes dialog when dirty.
- Made `No` in the Product Details unsaved-changes dialog explicitly discard the draft.
- Bumped and built releases through `2.7` / `versionCode 18`.

## Git And Release Notes

- Git repository: `E:\Comun\adi\Dev\Resell\Resell`
- Main branch: `main`
- Remote: `https://github.com/danadrianbaluta/Resell.git`
- Previous push attempts may fail if GitHub credentials are unavailable in the Codex environment. If push fails with an auth prompt issue, the commit is still local and can be pushed from a normal authenticated terminal.

## Editing / Workflow Preferences

- Use `rg` for searching.
- Use `apply_patch` for hand edits when available. In this Windows multi-root workspace, `apply_patch` has sometimes failed with a sandbox wrapper error; small PowerShell `Set-Content` edits have been used as fallback.
- Do not revert unrelated user changes.
- Keep UI edits narrowly scoped to the requested screen.
- After Kotlin/Compose changes, run `./gradlew.bat assembleDebug` when practical.
- For release requests:
  - Increment `versionCode` by 1.
  - Increment `versionName` one minor step by the existing pattern.
  - Run `./gradlew.bat assembleRelease`.
  - Verify `app/build/outputs/apk/release/output-metadata.json`.
