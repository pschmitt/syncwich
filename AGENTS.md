# AGENTS.md

Repository instructions for AI coding agents working on Syncwich.

## Project shape

Syncwich is a Kotlin/Jetpack Compose Android app: a beautiful, Material You, **offline-first**
client for a self-hosted [Mealie](https://mealie.io) recipe manager, with cached browsing and
supported recipe, cookbook, meal-plan, and shopping-item editing.
Package `dev.pschmitt.syncwich`, debug applicationId `dev.pschmitt.syncwich.debug`, GPL-3.0.
Single `:app` Gradle module - this app doesn't need a multi-module split.

## Task tracking

- `TODO.md` is the running backlog/changelog for this project, one `## SW-N:` entry per feature
  or fix, numbered sequentially (never reuse or renumber an id). Each entry has a checklist of
  sub-items (`- [ ]`/`- [x]`) and ends with a `Status:` line (`not started` / `in progress` /
  `mostly done` / `**done**`, plus a date and how it was verified).
- Before starting any non-trivial new feature or fix, add (or update) an `SW-N` entry describing
  it - even if the same conversation immediately goes on to implement it. Update the
  checklist/status as work actually lands, rather than writing the whole entry retroactively once
  everything's finished. This keeps `TODO.md` an accurate record of what's done vs. still open,
  and lets another agent (or a future you) resume the work cold from just this file.
- Trivial one-off asks (a typo, a single-line tweak) don't need their own entry.
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

- `nix develop` provides the full toolchain (JDK 21, Android SDK, `just`, `ktfmt`) and installs
  the repo's pre-commit hooks (see `flake.nix`'s `git-hooks.nix` integration - trailing
  whitespace, EOF fixer, merge-conflict/large-file checks, `nixfmt`, `statix`). The generated
  `.pre-commit-config.yaml` is gitignored - it's regenerated from `flake.nix` on every shell
  entry, don't hand-edit it.
- Prefer the `justfile` recipes over raw `./gradlew`/`ssh`/`adb` invocations - run `just --list`
  for the full set.

## Builds

- **Never run Gradle builds locally on this machine** - always build on `rofl-13.brkn.lol` or
  `rofl-14.brkn.lol` instead. The `justfile` automates this (`just sync`, `just gradle`,
  `just build [variant]`, `just lint`, `just test`, `just check`, `just fetch`,
  `just build-fetch`), namespaced per git worktree so parallel agents don't clobber each other's
  remote sync directory mid-build.
- Release builds are signed with the persistent CI keystore, fetched from the rbw entry
  `"Syncwich CI Signing Keystore"` (create it before the first release build - see justfile's
  `build variant=release` comment).
- **CI is the sole authority on lint/format** - not `just lint`, not local judgment. If CI's
  `Lint` job fails, `.github/workflows/lint.yaml`'s `ktfmt` job auto-uploads a `ktfmt-diff-patch`
  artifact whenever `ktfmtCheck` fails, containing exactly what `./gradlew ktfmtFormat` would
  change. Grab it with `gh run download <run-id> -n ktfmt-diff-patch` and apply it (`git apply`)
  rather than guessing or reformatting by hand. The workflow deliberately does not create
  formatting PRs or push an agent-generated branch automatically; apply the patch on the
  intended branch and commit it directly. Fix every lint/format violation CI reports before
  calling a change done, even in files the current change didn't touch or author.
- Never tag a release from a commit whose CI hasn't gone green.

## Physical test devices

- **Zenfone 10** (`arm64-v8a`), connected directly over USB to this machine's adb:
  `just zenfone-install <apk>`, `just zenfone-uninstall [pkg]`, `just zenfone-logcat [filter]`,
  `just deploy-zenfone [variant]`.
- **Mi Pad 4** (`arm64-v8a`, rooted), reachable via SSH at `mi-pad-4.lan` port `8022` (Termux):
  `just mipad-install <apk>`, `just mipad-uninstall [pkg]`, `just mipad-logcat [filter]`,
  `just deploy-mipad [variant]`.
- **Pixel 5** (`arm64-v8a`, codename `redfin`), wireless adb at `px5.lan`, enabled on demand via
  `zhj adb::connect px5.lan`: `just px5-install <apk>`, `just px5-uninstall [pkg]`,
  `just px5-logcat [filter]`, `just deploy-px5 [variant]`.
- **Always deploy to every attached adb device** after landing a verified change: run
  `just deploy-all [variant]` rather than a single-device recipe, unless reproducing a
  device-specific bug.
- Signature mismatch gotcha: if a device already has a build signed with a different key, install
  fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Fix is `just <device>-uninstall` then install
  fresh - this wipes local app data (Room DB cache, stored server URL/token). Confirm with the
  user before doing this if it's not their own throwaway data.

## Architecture

- **Offline-first is a hard requirement of this app, not a nice-to-have.** It must stay fully
  usable with zero connectivity for anything already synced. Every read path - a screen, a
  ViewModel, a repository - reads from a Room `Flow` first; a network call is only ever a
  best-effort background *refresh* that upserts into Room, and its failure surfaces as a subtle
  staleness indicator (or is silently skipped) rather than blocking or replacing what's already
  cached. A feature that only works while the Mealie server is reachable, with no cached
  fallback, is a regression - not a reasonable first-pass scope-down.
- **Auth is a pasted long-lived Mealie API token, not username/password.** Mealie's password-login
  endpoint (`/api/auth/token`) issues a JWT that expires in ~48h - a bad fit for an app that may go
  days without a network connection. Onboarding asks for the server base URL and a long-lived API
  token (generated by the user in Mealie's own Profile → API Tokens page). No password is ever
  stored on-device. The server URL is never hardcoded anywhere in the app or its tests - it's a
  per-user setting, since Syncwich is a generic Mealie client, not tied to any one instance.
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
