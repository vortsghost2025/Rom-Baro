# Architecture

Clean-ish layered architecture. Nothing fancy — optimized for one solo dev to keep
the whole graph in their head.

```
┌──────────────────────────────────────────────────────────┐
│  UI layer                                                │
│  - Leanback fragments (TV)                               │
│  - Jetpack Compose screens (phone/tablet)                │
│  - ExoPlayer PlayerView host                             │
└──────────────────────────┬───────────────────────────────┘
                           │ StateFlow<UiState>
┌──────────────────────────▼───────────────────────────────┐
│  ViewModel layer (one VM per screen, Hilt-injected)      │
└──────────────────────────┬───────────────────────────────┘
                           │ suspend fns / Flow
┌──────────────────────────▼───────────────────────────────┐
│  Repository layer                                        │
│  - PlaylistRepository (M3U + Xtream → Channel entities)  │
│  - EpgRepository      (XMLTV  → Programme entities)      │
│  - FavoritesRepository                                   │
└──────────┬─────────────────────────────┬─────────────────┘
           │                             │
┌──────────▼──────────┐         ┌────────▼──────────────────┐
│  Data sources       │         │  Room database            │
│  - XtreamApi (OkHttp)│        │  - channels, programmes,  │
│  - M3UParser        │         │    playlists, favorites   │
│  - XmlTvParser      │         └───────────────────────────┘
└─────────────────────┘
```

## Key decisions

### Why Media3 / ExoPlayer?
Handles HLS, DASH, SmoothStreaming, MPEG-TS (the bread-and-butter of IPTV) with
adaptive bitrate, subtitles, and TrackSelector hooks. The legacy `com.google.android.exoplayer2`
is EOL — use `androidx.media3.*` only.

### Why both Leanback and Compose?
- TV: Leanback's BrowseSupportFragment gives you the canonical "row of cards" UX
  for free with proper D-pad focus management. Compose-for-TV is workable but still
  rough on focus edges; using Leanback for v1 is the boring correct choice.
- Phone/tablet: Compose is fast to build and looks modern.

`MainActivity` checks `UiModeManager.currentModeType == UI_MODE_TYPE_TELEVISION`
and routes accordingly.

### Why Room?
EPG payloads are large (a week of XMLTV for 500 channels = tens of MB of XML).
Parse once, query forever. Room gives us a typed DAO and reactive Flows.

### Threading
- Network + parsing: `Dispatchers.IO`
- DB writes: Room handles it
- UI: `viewModelScope` + `StateFlow`

## Extension points

| You want to add… | Touch these files |
|---|---|
| A new playlist source format | `data/` new package + `PlaylistRepository.import()` |
| Catchup playback | `XtreamApi.timeshiftUrl()` + `PlayerActivity` |
| A new EPG source | `data/epg/` + `EpgRepository` |
| Picture-in-Picture | `PlayerActivity.onUserLeaveHint()` |
| Cast support | Add `media3-cast` module + cast button in PlayerActivity |
| Recording / DVR | New `data/dvr/` module; needs Storage Access Framework |

## What's intentionally NOT here yet

- No analytics SDK. Add Firebase or PostHog when you have users.
- No crash reporter. Add Sentry / Crashlytics post-MVP.
- No tests beyond a couple parser unit tests. UI tests on Leanback are painful;
  prioritize manual QA on a real Fire TV stick.
- No obfuscation. Turn on R8 + resource shrinking when you ship release builds.
