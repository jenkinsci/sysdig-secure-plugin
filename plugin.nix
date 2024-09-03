{ maven }:

maven.buildMavenPackage {
  pname = "sysdig-secure-plugin";
  version = "2.3.4-SNAPSHOT";
  src = ./.;
  mvnHash = "sha256-MawsZtPS2k3CK9HkVxSMVcT1oUXvZLGuxJpSbN8lsYM=";

  doCheck = false;
  installPhase = "
    install -Dm644 target/sysdig-secure.hpi -t $out
    install -Dm644 target/sysdig-secure.jar -t $out
  ";
}
