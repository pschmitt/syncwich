# Syncwich

<p align="center">
  <img src="docs/branding/syncwich-icon-preview.png" alt="Syncwich icon" width="160" height="160" style="border-radius: 50%;">
</p>

<p align="center"><strong>A beautiful, offline-first Material You client for your own Mealie server.</strong></p>

Syncwich connects to a self-hosted [Mealie](https://mealie.io) instance and syncs your recipes,
meal plans, shopping lists, and cookbooks for offline-first browsing and editing. No Syncwich
account, no ads, no tracking - your data stays between your phone and your own server.

Syncwich is an independent Mealie client. It is still under active development; see
[TODO.md](TODO.md) for the remaining backlog.

## Features

- Home dashboard with sync status, favorites, recently viewed, and recent recipes
- Browse and search recipes, with category and tag filters in a bottom sheet
- Create, edit, import, and delete recipes, cookbooks, meal-plan entries, and shopping items
- Rich recipe detail: ingredients, required tools, steps, images, servings, nutrition, and metadata
- Recipe image galleries, inline step images, offline image caching, and a full-screen steps view
- Recipe actions including favorites, ratings, “I made this”, timeline events, sharing, and browser links
- Import recipes from URLs and open shared Mealie URLs/assets in Syncwich
- Meal plan calendar and shopping lists
- Cookbooks and their recipe collections
- Customizable navigation bar, light/dark/automatic themes, font size, and sync policy
- Optional encrypted backups with password and schedule support
- Full Material You theming (dynamic, wallpaper-derived color on Android 12+)
- Cached content remains fully usable offline once synced - a network call is only ever a
  best-effort background refresh, never a requirement for browsing saved data

## Connecting to your Mealie server

Syncwich never asks for or stores your Mealie account password. Instead:

1. In Mealie, go to your user Profile → API Tokens and generate a long-lived token.
2. In Syncwich, enter your server's URL and paste that token.

The server URL is never hardcoded into the app - Syncwich works with any Mealie instance you
point it at. When Syncwich creates an API token on your behalf, it names it
`Syncwich (<DEVICE NAME>)` so it is easy to identify and revoke in Mealie.

## Install

Not published on Google Play or F-Droid yet. Install and auto-update via Obtainium, or download
an APK directly from the [releases page](https://github.com/pschmitt/syncwich/releases).

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="60">][obtainium-link]

The badge tracks the signed `release` APK from the rolling `latest` pre-release. Obtainium
selects the APK architecture automatically; the debug APKs remain available for development.

[obtainium-link]: https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22dev.pschmitt.syncwich%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fpschmitt%2Fsyncwich%22%2C%22author%22%3A%22pschmitt%22%2C%22name%22%3A%22Syncwich%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22syncwich-.%2A-app-.%2A-release%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22trackOnly%5C%22%3Afalse%7D%22%7D

Managing apps declaratively with [declaroid](https://github.com/pschmitt/declaroid) instead? Add:

```yaml
- name: "Syncwich"
  pkg: dev.pschmitt.syncwich
  store: github
  repo: pschmitt/syncwich
```

### Google Play publishing

The `Play Store Release` workflow publishes signed Android App Bundles to the internal-testing
track when a semantic-version tag such as `1.0.0` is pushed. Version codes are derived from the
tag, so each new semantic version produces a higher Play version code.

Before the first tag, complete the one-time setup:

1. In Google Cloud, create a project, enable the Google Play Developer API, and create a service
   account.
2. In Play Console, grant that service account access to Syncwich with permission to release to
   testing tracks, then download its JSON key.
3. Create a persistent upload keystore, enroll in Play App Signing during the first release, and
   keep the keystore safe. The same upload key must be used for every CI bundle.
4. Add these GitHub repository secrets: `PLAY_SERVICE_ACCOUNT_JSON`, `CI_KEYSTORE_BASE64`,
   `CI_KEYSTORE_PASSWORD`, `CI_KEY_ALIAS`, and `CI_KEY_PASSWORD`.
5. Finish the Play Console app content, store listing, declarations, and internal-testers setup.

Privacy policy: [PRIVACY.md](https://github.com/pschmitt/syncwich/blob/main/PRIVACY.md).

Custom Mealie instances can associate their recipe and cookbook URLs with Syncwich using Android
App Links. See [docs/deep-links.md](docs/deep-links.md) for the required `assetlinks.json` and
server configuration.

## Development

Gradle builds intentionally run on `rofl-13` or `rofl-14`, not on the local workstation:

```sh
just check
just build debug
just deploy-all debug
```

`just check` runs ktfmt checks, unit tests, and Android Lint remotely. `just build-fetch debug`
builds remotely and copies the debug APK to `./dist`; `just deploy-all debug` then installs it on
every attached ADB device (Zenfone 10, Mi Pad 4, and Pixel 5 when connected). The debug
application id is `dev.pschmitt.syncwich.debug`.

Android instrumentation APKs can be compiled remotely with:

```sh
just gradle rofl-13.brkn.lol :app:assembleDebugAndroidTest
```

Do not run Gradle directly on the local workstation. See [AGENTS.md](AGENTS.md) for device
deployment, signing, offline-first architecture, and repository contribution conventions.

This project is licensed under [GPL-3.0](LICENSE) and is not affiliated with the Mealie project.
