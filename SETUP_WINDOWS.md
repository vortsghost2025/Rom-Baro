# Copying this scaffold to `S:\Rom Baro` and pushing to GitHub

Since I run in a sandbox I can't write directly to your Windows drive or push to
your repo. Two minutes of copy-paste and you're done.

## 1. Download the scaffold

Either:
- Download the workspace zip from the chat, OR
- Right-click → save each file from the preview

…and unpack into `S:\Rom Baro\` so the layout looks like:

```
S:\Rom Baro\
├── app\
├── build.gradle.kts
├── settings.gradle.kts
├── gradle\wrapper\gradle-wrapper.properties
├── README.md
└── ...
```

## 2. Generate the Gradle wrapper jar + scripts

The `gradlew` / `gradlew.bat` and `gradle-wrapper.jar` are binary; I omitted
them so the scaffold stays text-only and reviewable. Generate them with:

```powershell
cd "S:\Rom Baro"
# requires Gradle installed (https://gradle.org/install/)
gradle wrapper --gradle-version 8.7
```

If you don't have Gradle installed, open the folder in Android Studio (Koala+).
On first sync it will offer to "Use Gradle from: gradle-wrapper.properties" and
download everything for you.

## 3. First build

```powershell
cd "S:\Rom Baro"
.\gradlew.bat :app:assembleDebug
```

APK lands at `app\build\outputs\apk\debug\app-debug.apk`.

## 4. Push to GitHub (vortsghost2025/Rom-Baro)

```powershell
cd "S:\Rom Baro"
git init
git add .
git commit -m "Initial MVP scaffold"
git branch -M main
git remote add origin https://github.com/vortsghost2025/Rom-Baro.git
git push -u origin main
```

GitHub will prompt for credentials. Use a fresh PAT (NOT the one you pasted
earlier — that one should be revoked) or, better, set up SSH or GitHub CLI:
`gh auth login`.

## 5. Sideload to your Fire TV / Android TV

```powershell
adb connect 192.168.1.50:5555   # your TV's IP
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 6. First-run

Launch "Rom Baro" → Add Playlist → enter your friend's Xtream credentials → done.
