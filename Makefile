
update: update-parent-pom update-dependencies update-flake update-sysdig-cli-version

update-parent-pom:
	mvn versions:update-parent -DgenerateBackupPoms=false

update-dependencies:
	mvn versions:use-latest-versions

verify:
	mvn clean verify javadoc:jar

update-flake:
	-nix flake update

update-sysdig-cli-version:
	@echo "Fetching latest sysdig version…"
	@latest=$$(curl -sfSL https://download.sysdig.com/scanning/sysdig-cli-scanner/latest_version.txt) && \
	echo "Latest: $$latest" && \
	sed -i -E 's/(private static final String FIXED_SCANNED_VERSION = ")([^"]+)(")/\1'$$latest'\3/' src/main/java/com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/RemoteSysdigImageScanner.java && \
	echo "Version updated"
