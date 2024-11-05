{
  description = "Sysdig Secure Plugin for Jenkins";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:

    let
      overlays.default = final: prev: { plugin = final.callPackage ./plugin.nix { }; };

      setJavaVersion = final: prev: {
        jdk = prev.temurin-bin-17;
        jdt-language-server = prev.jdt-language-server.override { jdk = prev.jdk; };
      };

      flake = flake-utils.lib.eachDefaultSystem (
        system:
        let
          pkgs = import nixpkgs {
            inherit system;
            overlays = [
              setJavaVersion
              self.overlays.default
            ];
          };
        in
        {
          packages = {
            inherit (pkgs) plugin;
            default = pkgs.plugin;
          };
          devShells.default = pkgs.mkShell {
            packages = with pkgs; [
              jdt-language-server
              maven
              jdk
            ];
          };
          formatter = pkgs.nixfmt-rfc-style;
        }
      );
    in
    flake // { inherit overlays; };
}
