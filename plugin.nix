{ maven }:

maven.buildMavenPackage {
  pname = "sysdig-secure-plugin";
  version = "3.0.3";
  src = ./.;
  mvnHash = "sha256-5l+4glCPIItHC4YIWtxNf0quUwf+9/xWYunaVSjO2rQ=";

  doCheck = false;
  installPhase = "
    install -Dm644 target/sysdig-secure.hpi -t $out
    install -Dm644 target/sysdig-secure.jar -t $out
  ";
}
