# AGENTS.md

Repository instructions for AI coding agents working on Syncwich.

See `.just/android-app-ci/AGENTS-shared.md` for the fleet-wide task-tracking convention, dev
environment (`nix develop`/`git-hooks.nix`), CI-is-the-sole-lint-authority rule, and physical test
device docs (this app has all three: Zenfone 10, Mi Pad 4, Pixel 5) - read it alongside this file,
not instead of it.

## Project shape

Syncwich is a Kotlin/Jetpack Compose Android app: a beautiful, Material You, **offline-first**
client for a self-hosted [Mealie](https://mealie.io) recipe manager, with cached browsing and
supported recipe, cookbook, meal-plan, and shopping-item editing.
Package `dev.pschmitt.syncwich`, debug applicationId `dev.pschmitt.syncwich.debug`, GPL-3.0.
Single `:app` Gradle module - this app doesn't need a multi-module split.

## Task tracking

- This project's `TODO.md` prefix is `SW-N`.
- Any user message that starts with `todo: ` (case-insensitive) is a direct instruction to add a
  new `SW-N` entry to `TODO.md` for whatever follows the prefix, rather than acting on it
  immediately - file the backlog entry (not started, with a checklist inferred from the ask) and
  confirm back to the user, instead of implementing it in that turn.
- Keep `README.md` aligned with the current user-facing behavior, setup instructions, commands,
  links, screenshots/assets, and release process. When a code or configuration change makes the
  README stale, update it in the same change (and review the README before marking the related
  ticket done); do not defer documentation drift to an unspecified later pass.

## Git publishing

- Do not open pull requests unless the user explicitly asks for one.
- When the user asks to publish and merge work, commit it on the working branch, merge it into
  `main`, and push `main` directly. Close any accidentally created pull request after the direct
  merge.

## Dev environment

See the shared doc for the `nix develop`/`git-hooks.nix` basics. Prefer the `justfile` recipes
over raw `./gradlew`/`ssh`/`adb` invocations - run `just --list` for the full set.

## Builds

- **Never run Gradle builds locally on this machine** - always build on `rofl-13.brkn.lol` or
  `rofl-14.brkn.lol` instead (`just sync`, `just gradle`, `just build [variant]`, `just lint`,
  `just test`, `just check`, `just fetch`, `just build-fetch`).
- Release builds are signed with the persistent CI keystore, fetched from the rbw entry
  `"Syncwich CI Signing Keystore"` (create it before the first release build - see justfile's
  `build variant=release` comment).
- See the shared doc for the CI-lint-authority rule and `ktfmt-diff-patch` retrieval procedure.
  One difference here: `lint.yaml`'s ktfmt job deliberately does not create formatting PRs or
  push an agent-generated branch automatically (unlike the sibling apps) - apply the patch on the
  intended branch and commit it directly instead.

## Physical test devices

See the shared doc - this app has recipes for all three fleet devices (Zenfone 10, Mi Pad 4,
Pixel 5), default to `just deploy-all [variant]`.

## Architecture

- **Offline-first is a hard requirement of this app, not a nice-to-have.** It must stay fully
  usable with zero connectivity for anything already synced. Every read path - a screen, a
  ViewModel, a repository - reads from a Room `Flow` first; a network call is only ever a
  best-effort background *refresh* that upserts into Room, and its failure surfaces as a subtle
  staleness indicator (or is silently skipped) rather than blocking or replacing what's already
  cached. A feature that only works while the Mealie server is reachable, with no cached
  fallback, is a regression - not a reasonable first-pass scope-down.
- **Auth never stores a Mealie or identity-provider password.** Users can paste a long-lived Mealie
  API token, use the password-login endpoint once to mint that token, or use Mealie's server-side
  OIDC Authorization Code + PKCE flow. OIDC returns a short-lived Mealie JWT; it is stored encrypted
  and refreshed through `/api/auth/refresh` when it nears expiry. The server URL is never hardcoded
  anywhere in the app or its tests - it is a per-user setting, since Syncwich is a generic Mealie
  client, not tied to any one instance.
- Mealie API access via Retrofit + kotlinx.serialization, with a dynamic base-URL interceptor
  (reads the stored server URL at request time) and an auth interceptor (reads the stored API
  token) - same pattern as the sibling nyetbox app's `DynamicBaseUrlInterceptor`.
- Offline cache via Room. Recipe list/filter fields are real columns; the full recipe detail
  response (ingredients, instructions, nutrition) is stored as a JSON column and decoded with
  kotlinx.serialization at read time - deliberately not deep-normalized into many join tables.
  Recipe images are cached via Coil's disk cache, not duplicated into Room.
- **Before writing JSON-parsing code against a Mealie endpoint whose exact response shape you
  don't already have confirmed in this repo, query the user's real instance first** (base URL +
  credentials from the rbw item `"Mealie (AI Agent)"`, read-only `GET`s only - never write/mutate
  against it) rather than relying on Mealie's public docs, which can drift from a real server's
  actual API version. Never hardcode that URL anywhere in the app itself.
- Markdown rendering (`multiplatform-markdown-renderer`) for recipe descriptions/instructions,
  which Mealie stores as Markdown.
- Full Material You theming: dynamic, wallpaper-derived color on Android 12+
  (`dynamicLightColorScheme`/`dynamicDarkColorScheme`), falling back to a warm, food-inspired
  hand-picked palette (matching the launcher icon's terracotta) on older devices. Use
  `material-icons-extended` freely - icon+label for every button, menu item, and labeled row.

# vim: set ft=markdown et ts=2 sw=2 :
