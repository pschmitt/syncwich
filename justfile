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

zenfone_serial := env_var_or_default("ZENFONE_SERIAL", "R6AIB700W850L7G")

mipad_host := env_var_or_default("MIPAD_HOST", "mi-pad-4.lan")
mipad_ssh_port := env_var_or_default("MIPAD_SSH_PORT", "8022")
mipad_adb_port := env_var_or_default("MIPAD_ADB_PORT", "5555")

px5_host := env_var_or_default("PX5_HOST", "px5.lan")

# List all available recipes
default:
    @just --list

# --- Remote build (rofl-13 / rofl-14) -------------------------------------

# Sync the working tree to the remote build host (excludes .git/build/.gradle). The .git exclude
# has no trailing slash so it matches both a real .git/ directory (the main checkout) and a plain
# .git file (a linked worktree's gitlink, which points at a local-only .git/worktrees/... path
# that doesn't exist on the remote host and breaks `nix develop` there if it gets copied over).
sync host=remote_host:
    rsync -az --delete \
        --exclude='.git' --exclude='**/build/' \
        --exclude='.gradle/' --exclude='**/.gradle/' \
        ./ {{host}}:{{remote_path}}/

# Run one or more Gradle tasks on the remote host (syncs first)
gradle host=remote_host *tasks: (sync host)
    ssh {{host}} 'cd {{remote_path}} && nix develop --command ./gradlew {{tasks}}'

# Build an APK remotely. variant: debug (default) or release. Release builds are signed with the
# persistent CI keystore (fetched from the rbw entry "Syncwich CI Signing Keystore" and staged on
# the build host only for the duration of the build - create that rbw entry before the first
# release build). Without CI_KEYSTORE_*, Gradle silently signs with the host's throwaway
# ~/.android/debug.keystore and devices carrying CI-signed installs reject the APK with
# INSTALL_FAILED_UPDATE_INCOMPATIBLE.
build variant="debug" host=remote_host:
    #!/usr/bin/env bash
    set -euo pipefail
    git_revision=$(git describe --always --abbrev=12 --dirty --exclude=latest)
    build_date=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    if [[ "{{variant}}" != "release" ]]; then
      just sync "{{host}}"
      ssh "{{host}}" "
        export GIT_REVISION='$git_revision'
        export BUILD_DATE='$build_date'
        cd {{remote_path}} && nix develop --command ./gradlew ':app:assembleDebug'
      "
      exit 0
    fi
    if ! rbw unlocked >/dev/null 2>&1; then
      printf 'rbw is locked - run "rbw unlock" first (needed for the CI signing keystore)\n' >&2
      exit 2
    fi
    tmpdir=$(mktemp -d)
    trap 'rm -rf "$tmpdir"' EXIT
    rbw attachment get "Syncwich CI Signing Keystore" --attachment syncwich-ci.jks --output "$tmpdir/syncwich-ci.jks"
    rbw attachment get "Syncwich CI Signing Keystore" --attachment syncwich-ci-keystore.env --output "$tmpdir/syncwich-ci-keystore.env"
    just sync "{{host}}"
    ssh "{{host}}" 'mkdir -p ~/.syncwich-ci-tmp && chmod 700 ~/.syncwich-ci-tmp'
    scp -q "$tmpdir/syncwich-ci.jks" "$tmpdir/syncwich-ci-keystore.env" "{{host}}:.syncwich-ci-tmp/"
    # The keystore is shredded on the host whether or not the build succeeds.
    ssh "{{host}}" "
      artifact={{remote_path}}/app/build/outputs/apk/release/app-{{default_abi}}-release.apk
      previous_mtime=0
      [[ -f \"\$artifact\" ]] && previous_mtime=\$(stat -c %Y \"\$artifact\")
      set -a
      . ~/.syncwich-ci-tmp/syncwich-ci-keystore.env
      set +a
      export CI_KEYSTORE_PATH=\$HOME/.syncwich-ci-tmp/syncwich-ci.jks
      export GIT_REVISION='$git_revision'
      export BUILD_DATE='$build_date'
      cd {{remote_path}} && nix develop --command ./gradlew ':app:assembleRelease' --rerun-tasks 2>&1 | tee ~/syncwich-release-build.log
      rc=\$?
      if [[ \$rc -eq 0 && (! -f \"\$artifact\" || \$(stat -c %Y \"\$artifact\") -le \$previous_mtime) ]]; then
        echo 'release build did not refresh its APK artifact' >&2
        rc=1
      fi
      shred -u ~/.syncwich-ci-tmp/* 2>/dev/null || true
      rmdir ~/.syncwich-ci-tmp 2>/dev/null || true
      exit \$rc
    "

# Copy a built APK split back to ./dist locally. variant/host same as `build`, plus abi=<abi>
fetch variant="debug" host=remote_host abi=default_abi:
    #!/usr/bin/env bash
    set -euo pipefail
    mkdir -p {{local_dist}}
    scp "{{host}}:{{remote_path}}/app/build/outputs/apk/{{variant}}/app-{{abi}}-{{variant}}.apk" {{local_dist}}/

# Build an APK remotely and copy it back to ./dist. Same args as `build`.
build-fetch variant="debug" host=remote_host:
    just build {{variant}} {{host}}
    just fetch {{variant}} {{host}}

# ktfmt check via Gradle, remotely (mirrors .github/workflows/lint.yaml)
lint host=remote_host: (gradle host "ktfmtCheck")

# Run the unit test suite remotely
test host=remote_host: (gradle host ":app:testDebugUnitTest")

# ktfmtCheck + unit tests + Android Lint, remotely - the full local pre-push check.
check host=remote_host: (gradle host "ktfmtCheck :app:testDebugUnitTest lintDebug")

# Remote `./gradlew clean`
clean host=remote_host: (gradle host "clean")

# --- Zenfone 10 (USB, directly attached to this machine) -------------------

# Install an APK on the Zenfone 10 over adb (USB)
zenfone-install apk:
    adb -s {{zenfone_serial}} install -r {{apk}}

# Uninstall a package from the Zenfone 10. WARNING: wipes that app's local data (Room DB, saved
# server credentials).
zenfone-uninstall pkg=application_id:
    adb -s {{zenfone_serial}} uninstall {{pkg}}

# Tail logcat from the Zenfone 10, optionally filtered by a grep pattern
zenfone-logcat filter="":
    #!/usr/bin/env bash
    set -euo pipefail
    if [ -n "{{filter}}" ]; then
        adb -s {{zenfone_serial}} logcat | grep -i --line-buffered "{{filter}}"
    else
        adb -s {{zenfone_serial}} logcat
    fi

# Build an APK remotely, fetch it, and install it on the Zenfone 10. variant: debug (default) or release.
deploy-zenfone variant="debug":
    just build-fetch {{variant}}
    just zenfone-install "{{local_dist}}/app-{{default_abi}}-{{variant}}.apk"

# --- Mi Pad 4 (rooted, Termux SSH on port 8022) -----------------------------

# Run an arbitrary command on the Mi Pad 4 over SSH
mipad-ssh +cmd:
    ssh -p {{mipad_ssh_port}} {{mipad_host}} "{{cmd}}"

# Interactive shell on the Mi Pad 4
mipad-shell:
    ssh -p {{mipad_ssh_port}} {{mipad_host}}

# Find the port adbd is actually listening on (via `ss -ltnp` over root SSH), starting it as a
# fallback if it isn't running at all, then `adb connect` to it. Prints the resulting "host:port"
# adb target on stdout so other recipes can capture it - status/progress goes to stderr.
mipad-connect:
    #!/usr/bin/env bash
    set -euo pipefail
    port=$(ssh -p {{mipad_ssh_port}} {{mipad_host}} "su -c 'ss -ltnp'" 2>/dev/null \
        | awk '/adbd/ { n = split($4, a, ":"); print a[n]; exit }')
    if [ -z "$port" ]; then
        echo "adbd not listening - starting it via root shell" >&2
        ssh -p {{mipad_ssh_port}} {{mipad_host}} \
            "su -c 'setprop service.adb.tcp.port {{mipad_adb_port}} && stop adbd && start adbd'" >&2
        sleep 1
        port={{mipad_adb_port}}
    fi
    target="{{mipad_host}}:$port"
    adb connect "$target" >&2
    echo "$target"

# Install an APK on the Mi Pad 4 over adb (network, via mipad-connect). Simpler and more reliable
# than scp + `pm install`: adb push/install runs as adbd, which doesn't hit the SELinux/FUSE
# permission issues a plain scp into /sdcard runs into when system_server tries to read the file
# back.
mipad-install apk:
    #!/usr/bin/env bash
    set -euo pipefail
    target=$(just mipad-connect)
    adb -s "$target" install -r {{apk}}

# Uninstall a package from the Mi Pad 4. WARNING: wipes that app's local data.
mipad-uninstall pkg=application_id:
    #!/usr/bin/env bash
    set -euo pipefail
    target=$(just mipad-connect)
    adb -s "$target" uninstall {{pkg}}

# Tail logcat from the Mi Pad 4, optionally filtered by a grep pattern
mipad-logcat filter="":
    #!/usr/bin/env bash
    set -euo pipefail
    target=$(just mipad-connect)
    if [ -n "{{filter}}" ]; then
        adb -s "$target" logcat | grep -i --line-buffered "{{filter}}"
    else
        adb -s "$target" logcat
    fi

# Build an APK remotely, fetch it, and install it on the Mi Pad 4. variant: debug (default) or release.
deploy-mipad variant="debug":
    just build-fetch {{variant}}
    just mipad-install "{{local_dist}}/app-{{default_abi}}-{{variant}}.apk"

# --- Pixel 5 (px5.lan, wireless adb enabled on demand via Home Assistant/Tasker) -------------

# Enable wireless adb on the Pixel 5 (via `zhj adb::connect`, which triggers it through Home
# Assistant/Tasker) and connect. The port is dynamic (assigned fresh each time wireless debugging
# is (re)enabled), so this always re-discovers it from `adb devices` rather than assuming a fixed
# one - prints the resulting "host:port" target on stdout, status goes to stderr.
px5-connect:
    #!/usr/bin/env bash
    set -euo pipefail
    zhj adb::connect {{px5_host}} >&2
    target=$(adb devices | awk -v h="{{px5_host}}" '$1 ~ h { print $1; exit }')
    if [ -z "$target" ]; then
        echo "px5 (host {{px5_host}}) not found in \`adb devices\` after connecting" >&2
        exit 1
    fi
    echo "$target"

# Install an APK on the Pixel 5 over adb (wireless, via px5-connect)
px5-install apk:
    #!/usr/bin/env bash
    set -euo pipefail
    target=$(just px5-connect)
    adb -s "$target" install -r {{apk}}

# Uninstall a package from the Pixel 5. WARNING: wipes that app's local data.
px5-uninstall pkg=application_id:
    #!/usr/bin/env bash
    set -euo pipefail
    target=$(just px5-connect)
    adb -s "$target" uninstall {{pkg}}

# Tail logcat from the Pixel 5, optionally filtered by a grep pattern
px5-logcat filter="":
    #!/usr/bin/env bash
    set -euo pipefail
    target=$(just px5-connect)
    if [ -n "{{filter}}" ]; then
        adb -s "$target" logcat | grep -i --line-buffered "{{filter}}"
    else
        adb -s "$target" logcat
    fi

# Build an APK remotely, fetch it, and install it on the Pixel 5. variant: debug (default) or release.
deploy-px5 variant="debug":
    just build-fetch {{variant}}
    just px5-install "{{local_dist}}/app-{{default_abi}}-{{variant}}.apk"

# --- All devices -------------------------------------------------------------

# Build once, fetch once, install on every connected test device (Zenfone 10, Mi Pad 4, Pixel 5).
# The default target device for iterating on changes - see AGENTS.md.
deploy-all variant="debug":
    just build-fetch {{variant}}
    just zenfone-install "{{local_dist}}/app-{{default_abi}}-{{variant}}.apk"
    just mipad-install "{{local_dist}}/app-{{default_abi}}-{{variant}}.apk"
    just px5-install "{{local_dist}}/app-{{default_abi}}-{{variant}}.apk"

# --- Play Console uploads ---------------------------------------------------

play_package := "dev.pschmitt.syncwich"

# Upload the generated screenshots to the Play Console listing. Deliberately separate from
# capturing them: review the images (build artifact, or the PR screenshots.yaml opens with
# open_pr) before this ever runs. Replaces each bucket's existing images with the local set rather
# than appending to it - the locally generated set is always the authoritative "current" one.
screenshots-upload:
    #!/usr/bin/env bash
    set -euo pipefail
    image_dir="fastlane/metadata/android"
    shopt -s nullglob
    image_types=(phoneScreenshots sevenInchScreenshots tenInchScreenshots)
    found_images=0
    for image_type in "${image_types[@]}"
    do
      image_glob=("$image_dir"/en-US/images/"$image_type"/*)
      if [[ ${#image_glob[@]} -gt 0 ]]
      then
        found_images=1
      fi
    done
    if [[ "$found_images" -eq 0 ]]
    then
      printf 'No generated screenshots found under %s\n' "$image_dir" >&2
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
    for image_type in "${image_types[@]}"
    do
      image_glob=("$image_dir"/en-US/images/"$image_type"/*)
      [[ ${#image_glob[@]} -gt 0 ]] || continue
      gpc --package {{play_package}} images delete-all --locale en-US --type "$image_type" --confirm
      for image in "${image_glob[@]}"
      do
        printf 'Uploading %s\n' "$image"
        gpc --package {{play_package}} images upload \
          --locale en-US \
          --type "$image_type" \
          --file "$image"
      done
    done

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

# --- Local formatting -------------------------------------------------------

# Standalone ktfmt CLI over all tracked .kt/.kts files - fast, but advisory only (see flake.nix's
# comment on why there's no ktfmt pre-commit hook). Confirm with `just lint` before relying on it.
format:
    ktfmt --kotlinlang-style $(git ls-files '*.kt' '*.kts')

nix-fmt:
    nix develop --command nixfmt flake.nix

nix-lint:
    nix develop --command statix check

# vim: set ft=sh et ts=2 sw=2 :
