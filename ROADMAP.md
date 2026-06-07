# Roadmap

## ✅ MVP (this scaffold)
- [x] Project structure, Gradle, Hilt, Room
- [x] M3U parser
- [x] Xtream Codes API client (player_api.php)
- [x] XMLTV parser (gzip-aware)
- [x] Room schema for playlists, channels, programmes, favorites
- [x] Leanback browse UI (TV)
- [x] Compose phone/tablet UI (placeholder list)
- [x] ExoPlayer host activity
- [x] Playlist setup screen

## 🚧 v0.2 — "Daily driver"
- [ ] Background EPG refresh (WorkManager, every 12h)
- [ ] Channel grouping by category
- [ ] Search across channels + EPG
- [ ] Favorites screen on TV (first Leanback row)
- [ ] Now-Playing EPG strip overlay during playback
- [ ] Settings: stream user-agent override, buffer size, hardware decoder toggle

## 🚧 v0.3 — Feature parity push
- [ ] Catchup / timeshift playback (Xtream `timeshift/` endpoint)
- [ ] Grid EPG (24h × N channels, D-pad scrollable)
- [ ] VOD + Series sections with detail screen
- [ ] Parental PIN per category
- [ ] Multi-playlist switcher
- [ ] Picture-in-Picture
- [ ] External player handoff (VLC, MX Player)

## 🚧 v1.0 — Polish & ship
- [ ] Onboarding wizard with QR code playlist import
- [ ] Companion phone app: scan QR on TV to send playlist
- [ ] Theming (accent color, channel logo border style)
- [ ] Telemetry opt-in
- [ ] Release signing config, GitHub Actions APK release on tag
- [ ] Crashlytics or Sentry
- [ ] Play Store listing (expect rejection cycles — have sideload page ready)
- [ ] Lightweight licensing server for premium tier (optional)

## 🔮 Later
- [ ] Google Cast
- [ ] Chromecast with Google TV optimized banner
- [ ] tvOS / Apple TV companion (separate codebase, Swift)
- [ ] Web player (Next.js + hls.js) for desktops
- [ ] Whitelabel build flavor system so a reseller can ship branded APK
