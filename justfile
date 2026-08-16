# Syncwich task runner.
#
# Gradle must never run on this machine directly - every build/test/lint recipe here shells out to
# a remote host (rofl-13.brkn.lol or rofl-14.brkn.lol) over SSH instead. See AGENTS.md.

set shell := ["bash", "-euo", "pipefail", "-c"]

application_id := "dev.pschmitt.syncwich"

remote_host := env_var_or_default("SYNCWICH_REMOTE_HOST", "rofl-13.brkn.lol")

# Empty for the main checkout; "-<worktree-dirname>" when run from a linked git worktree (e.g. one
# of Claude's isolated agent worktrees under .claude/worktrees/). Keeps parallel worktree agents
# from clobbering each other's remote sync directory mid-build.
worktree_suffix := `gd=$(git rev-parse --git-dir); gcd=$(git rev-parse --git-common-dir); if [ "$gd" != "$gcd" ]; then basename "$(git rev-parse --show-toplevel)" | sed 's/^/-/'; fi`

remote_path := env_var_or_default("SYNCWICH_REMOTE_PATH", "~/build/syncwich" + worktree_suffix)
local_dist := env_var_or_default("SYNCWICH_DIST_DIR", "./dist")

default_abi := env_var_or_default("SYNCWICH_ABI", "arm64-v8a")
gradle_extra_props := ""

zenfone_serial := env_var_or_default("ZENFONE_SERIAL", "R6AIB700W850L7G")

mipad_host := env_var_or_default("MIPAD_HOST", "mi-pad-4.lan")
mipad_ssh_port := env_var_or_default("MIPAD_SSH_PORT", "8022")
mipad_adb_port := env_var_or_default("MIPAD_ADB_PORT", "5555")

px5_host := env_var_or_default("PX5_HOST", "px5.lan")

# Release builds are signed with the persistent CI keystore, fetched from this rbw entry (create
# it before the first release build) and staged on the build host only for the duration of the
# build. Without CI_KEYSTORE_*, Gradle silently signs with the host's throwaway
# ~/.android/debug.keystore, and devices carrying CI-signed installs reject the APK with
# INSTALL_FAILED_UPDATE_INCOMPATIBLE.
enable_release_signing := "true"
rbw_keystore_entry := "Syncwich CI Signing Keystore"
keystore_jks_attachment := "syncwich-ci.jks"
keystore_env_attachment := "syncwich-ci-keystore.env"
ci_tmp_dir_name := ".syncwich-ci-tmp"

# List all available recipes. Must stay the first recipe in this file (not just the first line
# overall) - `just` only considers recipes written directly here, not ones pulled in via the
# import below, when deciding what a bare `just` invocation runs.
default:
    @just --list

# Recipes shared across the app fleet: format/nix-fmt/nix-lint/screenshots-upload (common.just, all
# 4 apps) and the remote sync/build/deploy pipeline - sync/gradle/build/fetch/build-fetch/clean/
# lint/test plus the zenfone-*/mipad-*/px5-*/deploy-all device recipes (single-module.just, the 3
# single-Gradle-module apps). See pschmitt/android-app-ci's just/ for the source of truth.
# Pulled in via a git submodule at .just/android-app-ci (tracking that repo's main branch);
# `just update-common` (defined at the bottom of this file) refreshes it. The devShell's shellHook
# auto-runs `git submodule update --init` on every `nix develop` entry, so a fresh git worktree
# never needs a manual `--init` step.
import '.just/android-app-ci/just/common.just'
import '.just/android-app-ci/just/single-module.just'

# ktfmtCheck + unit tests + Android Lint, remotely - the full local pre-push check.
check host=remote_host: (gradle host "ktfmtCheck :app:testDebugUnitTest lintDebug")

# --- Play Console uploads ---------------------------------------------------

play_package := "dev.pschmitt.syncwich"

# Flatten and upload the app icon used by the launcher and README (not locale-scoped, so kept
# separate from the screenshot upload above).
play-icon-upload:
    #!/usr/bin/env bash
    set -euo pipefail
    source_icon="app/src/main/res/mipmap/ic_launcher.png"
    if [[ ! -f "$source_icon" ]]
    then
      printf 'Icon source not found: %s\n' "$source_icon" >&2
      exit 1
    fi
    if ! command -v magick >/dev/null
    then
      printf 'ImageMagick `magick` is required to flatten the icon\n' >&2
      exit 1
    fi
    if ! command -v gpc >/dev/null
    then
      printf 'gpc (playconsole-cli) is required for Play Console uploads\n' >&2
      exit 1
    fi
    if ! gpc apps list --output json | rg -q '"package_name":"{{play_package}}"'
    then
      printf 'Play Console package %s was not found via `gpc apps list`\n' "{{play_package}}" >&2
      exit 1
    fi
    temp_dir=$(mktemp -d)
    trap 'rm -rf "$temp_dir"' EXIT
    magick "$source_icon" -resize 512x512 -alpha set "$temp_dir/syncwich-icon.png"
    gpc --package {{play_package}} images upload \
      --locale en-US \
      --type icon \
      --file "$temp_dir/syncwich-icon.png"

# Upload the already-committed feature graphic
# (fastlane/metadata/android/en-US/images/featureGraphic.png, 1024x500) to the Play Console
# listing. Not locale-scoped, so kept separate from the screenshot upload above.
play-feature-graphic-upload:
    #!/usr/bin/env bash
    set -euo pipefail
    graphic="fastlane/metadata/android/en-US/images/featureGraphic.png"
    if [[ ! -f "$graphic" ]]
    then
      printf 'Feature graphic not found: %s\n' "$graphic" >&2
      exit 1
    fi
    if ! command -v gpc >/dev/null
    then
      printf 'gpc (playconsole-cli) is required for Play Console uploads\n' >&2
      exit 1
    fi
    if ! gpc apps list --output json | rg -q '"package_name":"{{play_package}}"'
    then
      printf 'Play Console package %s was not found via `gpc apps list`\n' "{{play_package}}" >&2
      exit 1
    fi
    gpc --package {{play_package}} images upload \
      --locale en-US \
      --type featureGraphic \
      --file "$graphic"

# --- Shared recipes (pschmitt/android-app-ci) -------------------------------

# Advance the .just/android-app-ci submodule to the tip of its tracked branch (main) and stage the
# result - review the diff like any other dependency bump before committing it.
update-common:
    git submodule update --remote .just/android-app-ci
    git add .just/android-app-ci

# vim: set ft=sh et ts=2 sw=2 :
