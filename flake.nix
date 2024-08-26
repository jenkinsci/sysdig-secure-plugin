{
  description = "Sysdig Secure Plugin for Jenkins";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    nixpkgs-graalvm8.url = "github:NixOS/nixpkgs/3109ff5765505dbe1f7f2905ed3f54c62bd0acaa";
  };
  outputs =
    {
      self,
      nixpkgs,
      nixpkgs-graalvm8,
    }:
    let
      supportedSystems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];

      setJavaVersion = final: prev: {
        # We need to use a very old version of GraalVM 8 (from 2021-10-04), otherwise the plugin
        # does not pass the tests and does not compile with `release:prepare` and `release:perform`.
        jdk = graalvm8ForSystem prev.system;
        jdt-language-server = prev.jdt-language-server.override { jdk = prev.jdk; };
      };

      graalvm8ForSystem = system: (import nixpkgs-graalvm8 { inherit system; }).graalvm8-ce;

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
