# AGENTS.md — JagtFokus Android

Guidance for AI agents working in this repo.

The iOS app in `../jagtapp` is the behaviour north star. Read its `spec/` before product work. Do not fork that spec. Android-only design choices go in `spec/design.md` here, in Danish.

## Language

Talk to the user in Danish.

Write agent and developer docs in English. That includes this file and `README.md`.

Keep Danish for `spec/`. Quote iOS spec strings verbatim. Do not translate them.

The app ships two languages: **Danish** (source) and **English**. Danish lives in `app/src/main/res/values/`. English lives in `values-en/`. An unknown language must fall back to Danish, so Danish is the default resource, not English. Species names, place names, notes and biology come from the data file and stay Danish.

## What this is

Android app: **hvad er jagtbart her og nu?** Danish hunting seasons by date and place.

v1 of the iOS app, including the journal, is in `../jagtapp/spec/v1.md`. Design and UX are in `../jagtapp/spec/design.md`. Android divergences are in `spec/design.md`.

This repo is a port, not a rewrite of the product. Look at how the iOS app does a thing, then use the Android system pattern when the system cannot do the same thing.

## Stack

| Layer | Tech |
|---|---|
| App | Kotlin, Jetpack Compose, Material 3, minSdk 26, targetSdk 36, compileSdk 37, id `dk.jagtfokus.app` |
| Core | `:core`, pure Kotlin, no Android. JVM tests |
| Data | `data/jagttider.json`, copied from the iOS repo. Bundled as an asset |
| Tests | `./gradlew :core:test` (JUnit) |

Language: Danish and English. No account. No tracking. No Play Services.

## Layout

```
spec/design.md                     # Android divergences only. Danish
data/jagttider.json                # Copy of the iOS seasons file. Do not edit seasons here
core/src/main/kotlin/dk/jagtfokus/core/
  HuntingCatalog.kt                # Models and JSON
  SpeciesBiology.kt                # Biology catalog. Display only
  SpeciesPhotos.kt                 # Photo credits. Display only
  BagJournal.kt                    # The bag. Pure logic + codec, JVM tests
  SeasonQuery.kt                   # Places, overrides, matchPlace
  SeasonCalendar.kt                # Status and Copenhagen dates
  SeasonLists.kt                   # Open groups, closed species, search
  CatalogCheck.kt                  # Refuses a broken catalog
core/src/test/kotlin/
app/src/main/kotlin/dk/jagtfokus/app/
  MainActivity.kt
  data/CatalogLoader.kt
  location/LocationSource.kt
  ui/theme/Theme.kt                # Tokens. No hex in screens
  jagtbart/                        # The two screens
  data/BagFileStore.kt             # Bag files with backup and corrupt preservation
app/src/main/res/values/           # Danish source strings
app/src/main/res/values-en/        # English
```

## Screens

Built:

1. **Jagtbart** — heading “Jagtbart vildt efter tid og sted”. Date and place, then the sun card, then the answer card, then open groups, then “Ikke jagtbar her”. Default: today + here. Pull to refresh updates location and, when newer, the catalog.
2. **Sol og vejr** — pushed from the sun card. Display only. MET Norway. Sun and moon are calculated on the phone.
3. **Artsdetalje** — pushed from Jagtbart, so it shows status. Photo, biology and a **Tilføj til journal** row like iOS.
4. **Indstillinger** — fourth tab. Udseende (System/Lys/Mørk, no Material You), then the Om sections in the same list: Data, Ansvar, Schweiss-registret (pushed, same content as the sheet), Fotos, Privatliv, Denne app with Version and Dataversion. "Sammenfold grupper" lives here now (was a sheet until this tab existed).
5. **Schweiss-registret** — a sheet from the **Anskudt** button in the top bar; pushed from Indstillinger's Om section. No handler phone list, no Anskudt button on rows.
6. **Journal** — the second tab, so the bottom bar exists now. Its own date and place draft. Year rows, species list, search and edit, like iOS v1. Swipe left on a species row in Jagtbart records it and switches tabs; the row snaps back. Species detail has **Tilføj til journal**. Local file only, no cloud.
7. **Alle arter** — third tab. General seasons for every species, grouped cards that start collapsed (Sammenfold grupper applies; search unfolds groups with hits). No date, no place, no open/closed. Rows have the same bag swipe as Jagtbart. Detail page shows the general context: no status, thin SeasonBar without marker, species note.
8. **Velkomst** — first start only, before everything. Dark green brand world with the mark, three points (date and place, local seasons, private and offline), the disclaimer in a gold box, and **Forstået**. Explains the location use before the system dialog. Stored in the "velkomst" preference.

Not built yet: nothing left from the iOS spec.

Bottom bar: Jagtbart, Journal, Alle arter, Indstillinger.

## Rules

- Date and place decide the Jagtbart list. Not the time of day.
- Sun times are display only. No logic may use them. They are not in this slice.
- Biology is display only. No logic may use it.
- A local season beats the general one. Island > area > region > Denmark.
- An empty period means no season at that place.
- Notes are text, not logic.
- The app is not the law.
- Red deer and fallow deer: general seasons plus a note only. Map appendices are not in v1.
- Whole place names only. “Hals” must not match “Als”.

## UI rules

- Read `../jagtapp/spec/design.md` and `spec/design.md` before UI work. Update `spec/design.md` when Android diverges.
- Material 3. Do not clone an iOS list, sheet, or symbol.
- Use tokens from `ui/theme/Theme.kt` (`Spacing`, `Radius`, `LocalSeasonColors`). No hex in screens.
- Text uses the Material type scale and `sp`. It must follow the system font scale. Touch targets are at least 48 dp.
- Status is text plus an icon. Never color alone.
- Brand colors are fixed. Do not turn on Material You. Forest, amber and brass mean status.
- Logic lives in `:core` with tests. Screens only format. Jagtbart's lists come from `openGroups`, `closedSpecies` and `closingSoonCount`. Every search uses `TextSearch`.
- A component used on two screens lives under `ui/`. Do not copy a row from one feature to another.
- No glass on content. Only the system's bars and controls.
- Group icons are a temporary Material stand-in. The iOS line sketches are not ported.

## Copy and translation

Write the copy in Danish in `values/strings.xml`. Add the English line in `values-en/strings.xml` in the same change. Species names, place names and notes stay in the JSON.

A count that changes the words goes in `plurals`. Do not branch on `count == 1` in Kotlin.

`SeasonCalendar` takes a locale, so date text is testable. Pass `Locale.forLanguageTag("da-DK")` in tests. Never rely on the machine.

`SeasonCalendar` counts days in Europe/Copenhagen, and its text must too. Format through the calendar, never with the device zone. A phone west of Denmark must not show a season opening 1 October as “30. sep.”. CI may run in UTC.

## Run

JDK 21. `JAVA_HOME` must point at it. This machine:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew :core:test :app:assembleDebug
```

`local.properties` holds `sdk.dir` and is gitignored. Do not commit it.

Debug start, same idea as the iOS launch arguments. Force-stop first, or a running activity keeps the old extras:

```sh
adb shell am force-stop dk.jagtfokus.app
adb shell am start -n dk.jagtfokus.app/.MainActivity --es place als --es date 2026-11-15
```

The emulator has no GPS. A debug `place` extra skips “here”, so the list does not wait for a fix. A debug `sheet` extra opens a sheet, e.g. `--es sheet schweiss` for the Schweiss register.

An emulator (AVD `Pixel_10`) is installed on this machine and runs the app. `adb` is not on `PATH`; use `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`. Install and start:

```sh
ADB=/opt/homebrew/share/android-commandlinetools/platform-tools/adb
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
```

Smoke test of Artsdetalje without a finger: dump the UI to find a row, then tap. Groups start folded, so expand one first:

```sh
$ADB shell uiautomator dump /sdcard/ui.xml && $ADB shell cat /sdcard/ui.xml   # read text= and bounds=
$ADB shell input tap X Y
```

A good check is Kronhjort: photo, biology (latinsk navn, “I Danmark”, “Yngel”), Arter.dk link.

## Data

Source of truth: `../jagtapp/data/jagttider.json`. This repo keeps a copy in `data/jagttider.json` so it builds alone. Do not edit seasons here. Copy the file when the iOS data changes, and bump nothing else.

Biology and photo credits are copies too: `data/biologi.json` and `data/species-photos.json` come from `../jagtapp/App/JagtFokus/Resources/`, and the images from `../jagtapp/App/JagtFokus/Assets.xcassets/SpeciesPhotos/photo-<id>.imageset/` into `app/src/main/assets/photos/photo-<id>.jpg`. The tests pin every species to a taxon and a photo.

The same remote URL will overwrite the bundle later, only when `version` is newer:

`https://raw.githubusercontent.com/KennethEgholm/jagtapp/main/data/jagttider.json`

`decodeChecked` refuses a file with problems: no species, no `dk`, a repeated species or place id, a parent or override pointing at nothing, or a `source_url` that is not https on retsinformation.dk. The source link opens only through `sourceLink`.

Season days are Europe/Copenhagen. Tests pin that zone. Do not format with `ZoneId.systemDefault()`.

## Do not

- Encode footnotes as logic.
- Show open/closed on Alle arter, once it exists.
- Put a date or place picker on Alle arter.
- Scrape authority websites.
- Claim the app is the law.
- Use glass on content.
- Use photos without credit, or with an NC/ND license.
- Claim the app has reported the hunting bag to the authorities.
- Add Play Services, an account, or a tracking SDK.
- Share this core back into the iOS app. The iOS core stays Swift.
