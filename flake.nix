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
      setJavaVersion = final: prev: {
        jdk = prev.temurin-bin-17;
        jdt-language-server = prev.jdt-language-server.override { jdk = prev.jdk; };
      };
    in
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [
            setJavaVersion
          ];
        };
      in
      {
        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            jdt-language-server
            maven
            jdk
            pre-commit
          ];

          shellHook = ''
            pre-commit install
          '';
        };
        formatter = pkgs.nixfmt-rfc-style;
      }
    );

}
