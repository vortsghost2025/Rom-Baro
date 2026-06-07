# Rom Baro

A modern IPTV player for Android TV, Fire TV, phones, and tablets.
Built as a TiviMate-class client around the Xtream Codes API and standard M3U / XMLTV.

> **Status:** MVP scaffold. See `ROADMAP.md` for what's implemented vs. planned.

---

## Features (MVP)

- ✅ Import playlists via **Xtream Codes** (host + username + password) or raw **M3U URL**
- ✅ Live TV browsing with Leanback rows on TV, grid on phone/tablet
- ✅ ExoPlayer (Media3) playback — HLS, MPEG-TS, DASH, progressive
- ✅ XMLTV EPG ingest (gzip-aware) with local Room cache
- ✅ Favorites
- ✅ D-pad / remote-friendly focus handling
- 🚧 Catchup / timeshift (stubbed)
- 🚧 Grid EPG screen (stubbed)
- 🚧 VOD / Series detail screens (stubbed)
- 🚧 Multi-playlist, parental PIN, PiP (planned)

---

## Build

Requirements:
- Android Studio Koala+ (AGP 8.5+)
- JDK 17
- Android SDK 34, min SDK 21

```bash
./gradlew :app:assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## Sideload to a Fire TV / Android TV

1. On the device: Settings → My Fire TV → Developer Options → ADB Debugging ON
2. Note its IP (Settings → Network)
3. From your PC:

```bash
adb connect <device-ip>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Phone / tablet

Just install the same APK; the app detects form factor and routes to the phone UI.

---

## First-run setup

1. Launch the app → **Add Playlist**
2. Choose **Xtream Codes** and enter:
   - Server URL (e.g. `http://your-provider.tld:8080`)
   - Username
   - Password
   - (optional) XMLTV EPG URL — usually `http://your-provider.tld:8080/xmltv.php?username=...&password=...`
3. Hit **Save** → channels populate.

---

## Project layout

```
app/src/main/kotlin/com/rombaro/tv/
├── RomBaroApp.kt          # Application + Hilt entry
├── di/                    # Hilt modules (Network, Database, Repository)
├── data/
│   ├── m3u/               # M3U / M3U8 parser
│   ├── xtream/            # Xtream Codes REST client
│   ├── epg/               # XMLTV parser (handles .gz)
│   ├── db/                # Room: entities, DAOs, database
│   └── repo/              # PlaylistRepository, EpgRepository
├── domain/                # Pure Kotlin models (Channel, Programme, Playlist)
├── player/                # ExoPlayer host activity
└── ui/
    ├── MainActivity.kt    # Routes to TV or phone UI based on UiModeManager
    ├── browse/            # Leanback BrowseFragment (TV)
    ├── guide/             # EPG screens
    ├── settings/          # Playlist add/edit
    └── phone/             # Phone/tablet Compose UI
```

See **ARCHITECTURE.md** for the rationale and extension points.

---

## License

MIT — see `LICENSE`. Note: this app is a **neutral media player**. You are responsible
for the legality of any streams you load into it. Do not ship preloaded playlists.

---

## Credits

- AndroidX Media3 / ExoPlayer
- AndroidX Leanback
- Hilt, Room, Coroutines, kotlinx-serialization, OkHttp
