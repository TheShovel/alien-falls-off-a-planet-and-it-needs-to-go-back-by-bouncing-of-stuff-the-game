# Alien — Android release

## Identity

| Field | Value |
| --- | --- |
| Package name (applicationId) | `rocks.theshovel.alien` |
| Launcher / app label | `Alien` |
| Version name | `1.0.0` |
| Version code | `1` |
| Orientation | `portrait` |
| minSdk / targetSdk / compileSdk | 24 / 36 / 36 |
| Permissions | `INTERNET` only |

Change any of these in `app.properties`, then rebuild.

## Artifacts

| File | Purpose |
| --- | --- |
| `dist/alien-1.0.0-release.aab` | Upload this to Google Play (App bundle) |
| `dist/alien-1.0.0-release.apk` | Signed APK for sideloading / testers |

Both are signed with the `alien` upload key.

## Signing

- Keystore: `AndroidStudio/key.jks` (alias `alien`, RSA 4096, SHA384withRSA)
- Certificate SHA-256: `48:D7:7A:A6:6F:03:21:62:D3:CB:EC:B0:72:6B:51:BF:A4:38:A2:02:18:D1:D4:9D:BE:85:12:EB:8D:68:7A:BA`
- Certificate valid until 2054-04-02
- Password: `signing.properties` (gitignored)

Back up `AndroidStudio/key.jks` and `signing.properties` somewhere safe and
offline. Losing them means you cannot ship updates under the same listing
unless Play App Signing holds the app signing key.

## Play listing media

- `store/play-icon-512.png` — 512×512 app icon
- `store/play-feature-graphic-1024x500.png` — feature graphic
- Screenshots (min 2 phone screenshots, 16:9 or 9:16) still need to be taken
  on a device: `adb shell screencap -p /sdcard/shot.png && adb pull /sdcard/shot.png`

## Rebuild

```sh
export JAVA_HOME="$PWD/.tools/jdk-21.0.13+9"
export GRADLE_USER_HOME="$PWD/.tools/gradle-home"
./AndroidStudio/gradlew -p AndroidStudio assembleRelease bundleRelease
```

Bump `app.versionCode` (must increase every upload) and `app.versionName` in
`app.properties` before each new release.
