# Syncwich

<p align="center">
  <img src="docs/branding/syncwich-icon-preview.png" alt="Syncwich icon" width="160">
</p>

<p align="center"><strong>A beautiful, offline-first Material You client for your own Mealie server.</strong></p>

Syncwich connects to a self-hosted [Mealie](https://mealie.io) instance and syncs your recipes,
meal plans, shopping lists, and cookbooks for fully offline browsing. No Syncwich account, no
ads, no tracking - your data stays between your phone and your own server.

Currently **read-only**: browse and search, nothing more. Editing capabilities are planned for a
later release - see [TODO.md](TODO.md).

## Features

- Browse and search recipes, with category and tag filters
- Rich recipe detail: ingredients, steps, images, and nutrition
- Meal plan calendar (view)
- Shopping lists and their items (view)
- Cookbooks and their recipe collections (view)
- Full Material You theming (dynamic, wallpaper-derived color on Android 12+)
- Everything works fully offline once synced - a network call is only ever a best-effort
  background refresh, never a requirement

## Connecting to your Mealie server

Syncwich never asks for or stores your Mealie account password. Instead:

1. In Mealie, go to your user Profile → API Tokens and generate a long-lived token.
2. In Syncwich, enter your server's URL and paste that token.

The server URL is never hardcoded into the app - Syncwich works with any Mealie instance you
point it at.

## Install

Not published on Google Play or F-Droid yet. Install and auto-update via Obtainium, or download
an APK directly from the [releases page](https://github.com/pschmitt/syncwich/releases).

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

## Development

Gradle builds intentionally run on `rofl-13` or `rofl-14`, not on the local workstation:

```sh
just check
just build
just deploy-all
```

`just deploy-all` fetches the remote debug APK and installs it on every attached ADB device.
The debug application id is `dev.pschmitt.syncwich.debug`.

See [AGENTS.md](AGENTS.md) for repository conventions and [TODO.md](TODO.md) for the running
backlog. This project is licensed under [GPL-3.0](LICENSE) and is not affiliated with the Mealie
project.
