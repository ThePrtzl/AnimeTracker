# Anime Tracker

An Android app (Kotlin, Jetpack Compose) to browse anime, check off episodes, and keep a library with
per-season progress. The UI follows the designs picked during design review:

| Screen | Design |
|---|---|
| Browse | Deep Ink, 3-column poster grid, frosted info strip, genre filter, search |
| Detail | Poster on top, frosted sheet with synopsis and a progress ring per season |
| Episode sheet | Tap an episode to mark everything up to it; tap the last watched one to undo |
| Library | Grouped shelves: Started, Completed, Dropped (with "See all" and search) |

## Run it

1. Install **Android Studio Ladybug (2024.2.1) or newer**. It bundles the JDK 17 this project needs.
2. **File > Open** and choose this `AnimeTracker` folder. Let Gradle sync (first sync downloads dependencies).
3. Plug in a phone with USB debugging on, or create an emulator (API 26+), then press **Run**.

To build an installable APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
From a terminal: `./gradlew assembleDebug` (output in `app/build/outputs/apk/debug/`).
Run the unit tests with `./gradlew testDebugUnitTest`.

Android Studio may offer to upgrade the Android Gradle Plugin, Kotlin or library versions; that's fine.

## How tracking works

- **Seasons.** AniList lists every season as a separate entry linked by prequel/sequel relations. The app
  walks those links so one anime shows up once, with its seasons listed underneath. Movies, OVAs and
  specials are ignored.
- **Episodes.** Progress per season is stored as "episodes 1 to N watched". Tapping episode 9 marks 1 to 9;
  tapping the last watched episode un-marks it.
- **Season status** (automatic): nothing watched = Not started; some watched = Started; all watched (and the
  season has finished airing) = Completed; or Dropped when you tap *Drop season*.
- **Library status** (automatic): the anime is in your library once any season has progress.
  Any dropped season = **Dropped**; every season completed = **Completed**; anything else = **Started**.
  So "finished season 1, dropped season 2" appears under Dropped, and "finished season 1, started season 2"
  appears under Started.
- Everything you track is stored on the device (Room database). Anime info is cached so the library works offline.

## Data source

Anime info, posters and episode counts come from the [AniList](https://anilist.co) public GraphQL API
(no key needed). It allows about 90 requests per minute. Opening an anime with many seasons makes one request
per season the first time; after that it's cached. If you publish the app, check AniList's terms of use.

## Project layout

```
app/src/main/java/com/example/animetracker/
  AnimeApp.kt, MainActivity.kt        app entry, wiring
  domain/Models.kt                    Season / Franchise and the status rules
  data/remote/                        AniList client + DTOs
  data/local/                         Room entities, DAO, database
  data/AnimeRepository.kt             browse, season resolving, progress, library
  ui/theme, ui/components             Deep Ink theme and shared pieces
  ui/browse, ui/detail, ui/library    screens + view models (SeasonSheet.kt is the episode sheet)
  ui/AppRoot.kt                       navigation and bottom bar
app/src/test/.../DomainTest.kt        unit tests for the status rules and AniList helpers
```

## Known differences from the designs

- Compose has no cheap backdrop blur, so the "frosted" panels are a translucent dark fill with a light border.
- The bookmark button on the detail screen was left out: an anime joins the library as soon as you check
  off an episode.
- Posters use AniList cover art; while loading (or if missing) a colour gradient is shown instead.

## Not yet verified

This project was written without being able to compile it. Expect to fix a few small build errors on the
first sync, and please report anything that looks off on a real device.
