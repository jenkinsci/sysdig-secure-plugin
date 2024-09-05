{ maven }:

maven.buildMavenPackage {
  pname = "sysdig-secure-plugin";
  version = "2.3.4-SNAPSHOT";
  src = ./.;
  mvnHash = "sha256-JaXBP9xumsJTaS8kpob0Q0rer5hweF6fbODetOyqCAI=";

  doCheck = false;
  installPhase = "
    install -Dm644 target/sysdig-secure.hpi -t $out
    install -Dm644 target/sysdig-secure.jar -t $out
  ";
}
