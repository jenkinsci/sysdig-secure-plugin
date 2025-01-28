{ maven }:

maven.buildMavenPackage {
  pname = "sysdig-secure-plugin";
  version = "3.0.2";
  src = ./.;
  mvnHash = "sha256-BXnqTBE27wVP8JpWT6ed597CORhiHw6Ogc2tY6rNCIU=";

  doCheck = false;
  installPhase = "
    install -Dm644 target/sysdig-secure.hpi -t $out
    install -Dm644 target/sysdig-secure.jar -t $out
  ";
}
