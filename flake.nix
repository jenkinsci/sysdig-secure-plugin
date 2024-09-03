{
  description = "Sysdig Secure Plugin for Jenkins";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
  };
  outputs =
    { self, nixpkgs }:
    let
      supportedSystems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];

      setJavaVersion = final: prev: {
        jdk = prev.temurin-bin-17;
        jdt-language-server = prev.jdt-language-server.override { jdk = prev.jdk; };
      };

      forEachSystem =
        f:
        nixpkgs.lib.genAttrs supportedSystems (
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
          f pkgs
        );
    in
    {
      overlays.default = final: prev: { plugin = final.pkgs.callPackage ./plugin.nix { }; };

      packages = forEachSystem (
        pkgs: with pkgs; {
          inherit plugin;
          default = plugin;
        }
      );
      devShells = forEachSystem (
        pkgs: with pkgs; {
          default = mkShell {
            buildInputs = [
              jdt-language-server
              maven
              jdk
            ];
          };
        }
      );

      formatter = forEachSystem (pkgs: pkgs.nixfmt-rfc-style);
    };
}
