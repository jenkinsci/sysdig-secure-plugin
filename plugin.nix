{ maven }:

maven.buildMavenPackage {
  pname = "sysdig-secure-plugin";
  version = "2.3.5";
  src = ./.;
  mvnHash = "sha256-jE2eIfTMAK6euzk0G3W/DwO58dwo0FaxQsWky6irlJ8=";

  doCheck = false;
  installPhase = "
    install -Dm644 target/sysdig-secure.hpi -t $out
    install -Dm644 target/sysdig-secure.jar -t $out
  ";
}
