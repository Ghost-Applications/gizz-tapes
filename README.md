# Gizz Tapes

An Open Source King Gizzard and the Lizard Wizard music player, utilizing the
[Gizz Tapes](https://tapes.kglw.net/) API.

## Installation

### iOS

Get it from the App Store (iPhone and iPad).

<a href="https://apps.apple.com/app/gizz-tapes/id6761312548"><img src="https://toolbox.marketingtools.apple.com/api/v2/badges/download-on-the-app-store/black/en-us" alt="Download on the App Store" height="54"></a>

### Android

Available from Google Play, F-Droid, Obtainium, or directly from GitHub releases. There are two versions:

- **Full** (`gizz.tapes.full`): includes Chromecast support. Google Play, Obtainium, and GitHub.
- **FOSS** (`gizz.tapes.foss`): no Firebase or Chromecast libraries. F-Droid, Obtainium, and GitHub.

<a href="https://play.google.com/store/apps/details?id=gizz.tapes.full"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80"></a>
<a href="https://f-droid.org/en/packages/gizz.tapes.foss/"><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80"></a>
<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/Ghost-Applications/gizz-tapes"><img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a>
<a href="https://github.com/Ghost-Applications/gizz-tapes/releases"><img src="https://github.com/machiav3lli/oandbackupx/blob/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png?raw=true" alt="Get it on GitHub" height="80"></a>

We highly recommend using [Obtainium](https://github.com/ImranR98/Obtainium)
to install this app and to keep it up to date.

## Desktop

Desktop builds for macOS (`.dmg`), Windows (`.msi`), and Linux (`.deb`) are now attached to each
GitHub release. They're mainly a way to quickly test the UI and aren't officially supported. We
can't test every platform, so use them at your own risk.

The macOS build isn't notarized, so macOS will block it the first time you open it. To allow it,
go to System Settings → Privacy & Security and click "Open Anyway".

## Nightly Builds

Want to help us test? Every push to `main` publishes a
[nightly pre-release](https://github.com/Ghost-Applications/gizz-tapes/releases/tag/nightly) with
Android APKs (Full and FOSS) and desktop packages for macOS (`.dmg`), Windows (`.msi`), and Linux (`.deb`).
Nightlies may be unstable, so please [open an issue](https://github.com/Ghost-Applications/gizz-tapes/issues)
if you find something broken.

## Verifying Releases

Every GitHub release ships a detached PGP signature (`.asc`) alongside each APK, AAB, and desktop installer.

- PGP key: [`3239 DF00 5BDF 2CD2 7898  57AF 59F8 42F7 3BAF 2A8D`](https://ghostapps.rocks/pgp.asc)

```sh
gpg --recv-keys 3239DF005BDF2CD2789857AF59F842F73BAF2A8D
gpg --verify gizz-tapes-*-release.apk.asc gizz-tapes-*-release.apk
```

Signing certificate (SHA-256) for APKs from our GitHub releases. Compare it with
[Verified Apps](https://github.com/privacyguides/verified-apps-android),
[AppVerifier](https://github.com/soupslurpr/AppVerifier), or the certificate shown in Obtainium.

| Package ID | Certificate SHA-256 |
| --- | --- |
| `gizz.tapes.full`<br>`gizz.tapes.foss` | `CD:9A:8E:7D:FA:19:D4:C9:A2:3E:8A:46:51:3D:5B:71:53:E3:A0:72:8D:E6:AF:EF:A2:83:7B:85:36:B0:1E:51` |

> [!NOTE]
> F-Droid signs `gizz.tapes.foss` with its own key
> (`0A:32:1C:76:61:9C:8E:E5:CB:52:CD:42:9E:87:14:84:18:5E:7E:7C:95:B5:F3:4E:33:AA:16:E3:4C:2E:7F:13`), and Google
> Play builds are signed by Google Play App Signing, so those fingerprints differ from the one above.

## Building

### Setup

This project uses [mise](https://mise.jdx.dev/) to manage Java 21 and Ruby (for Fastlane).

Install mise, then run:

```bash
mise install
```

This will install the correct Java and Ruby versions automatically.

### Tasks

```bash
mise run test            # Run tests and static analysis
mise run build           # Build release and debug artifacts
mise run build-debug     # Assemble a debug APK
mise run install-debug   # Install a debug APK on a connected device
mise run release         # Release via Fastlane
```

### Build Scans

Add `systemProp.GIZZ_TAPES_ACCEPT_BUILD_SCAN_AGREEMENT=yes` to gradle.properties
to enable build scans on every build.

## License

```
Copyright 2026 Ghost Apps LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

Signed with our PGP key: [source](https://raw.githubusercontent.com/Ghost-Applications/gizz-tapes/main/README.md) · [signature](https://github.com/Ghost-Applications/gizz-tapes/blob/main/README.md.asc)
