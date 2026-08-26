{
  description = "Syncwich Android development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    # Pinned to a known-good revision: unpinned "latest" briefly shipped a broken cmdline-tools
    # wrapper (missing .android-wrapped), which broke `nix develop` fleet-wide since this repo had
    # no committed flake.lock. Sibling fleet repos (augh, nyetbox, jollyfin) pin the same input via
    # their own flake.lock - this uses the newest revision known to work there.
    android-nixpkgs = {
      url = "github:tadfisher/android-nixpkgs/50c0f56240ca8c9196c42b0f36ba8cc0b0398cfe";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    git-hooks = {
      url = "github:cachix/git-hooks.nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    android-app-ci = {
      url = "github:pschmitt/android-app-ci";
      flake = false;
    };
  };

  outputs =
    {
      self,
      nixpkgs,
      android-nixpkgs,
      git-hooks,
      android-app-ci,
    }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        config.allowUnfree = true;
      };

      androidEnv = import "${android-app-ci}/nix/devshells.nix" {
        inherit pkgs android-nixpkgs system;
        appName = "Syncwich";
        buildToolsVersion = "37.0.0";
        platformVersion = "37-0";
        gitHooksLib = git-hooks.lib;
        # No local AVD-based screenshot capture here (only CI-driven, see screenshots.yaml) - so
        # no `screenshots` devShell.
        screenshotsSystemImage = null;
        quickStart = ''
          echo "  just deploy-all               # ...and install it on every attached device"
        '';
      };
    in
    {
      devShells.${system} = androidEnv.devShells;
      checks.${system} = androidEnv.checks;
    };
}
