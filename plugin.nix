{ maven }:

maven.buildMavenPackage {
  pname = "sysdig-secure-plugin";
  version = "2.3.5";
  src = ./.;
  mvnHash = "sha256-uJ86guTCEN/vmrBK5hKVIPeQtuOsWCX+NmdwJ54vMmk=";

  doCheck = false;
  installPhase = "
    install -Dm644 target/sysdig-secure.hpi -t $out
    install -Dm644 target/sysdig-secure.jar -t $out
  ";
}
