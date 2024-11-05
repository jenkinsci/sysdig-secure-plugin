{ maven }:

maven.buildMavenPackage {
  pname = "sysdig-secure-plugin";
  version = "3.0.0";
  src = ./.;
  mvnHash = "sha256-jobIzi1COLEKoPiYc1Qqlp9orqrqyJmB06RKazGPVFQ=";

  doCheck = false;
  installPhase = "
    install -Dm644 target/sysdig-secure.hpi -t $out
    install -Dm644 target/sysdig-secure.jar -t $out
  ";
}
