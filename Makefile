
update: update-jenkins-version update-parent-pom update-dependencies update-flake update-sysdig-cli-version

update-jenkins-version:
	@echo "Fetching latest unmaintained LTS Jenkins version..."
	@version=$$(curl -s https://endoflife.date/api/v1/products/jenkins/ | jq -r '.result.releases | map(select(.isMaintained == false)) | first | .latest.name') && \
	baseline=$$(echo $$version | cut -d. -f1-2) && \
	echo "Found Jenkins version: $$version (Baseline: $$baseline)" && \
	echo "Fetching latest BOM version for bom-$$baseline.x..." && \
	bom_version=$$(curl -s "https://repo.jenkins-ci.org/public/io/jenkins/tools/bom/bom-$$baseline.x/maven-metadata.xml" | grep -oPm1 "(?<=<latest>)[^<]+") && \
	echo "Found BOM version: $$bom_version" && \
	sed -i 's|<jenkins.baseline>.*</jenkins.baseline>|<jenkins.baseline>'$$baseline'</jenkins.baseline>|' pom.xml && \
	sed -i 's|<jenkins.version>.*</jenkins.version>|<jenkins.version>'$$version'</jenkins.version>|' pom.xml && \
	sed -i "/<artifactId>bom-\$${jenkins.baseline}.x<\/artifactId>/,+2 s|<version>.*</version>|<version>$$bom_version</version>|" pom.xml && \
	echo "Updated pom.xml to Jenkins $$version, Baseline $$baseline, BOM $$bom_version"

update-parent-pom:
	mvn versions:update-parent -DgenerateBackupPoms=false

update-dependencies:
	mvn versions:use-latest-versions

verify:
	mvn clean spotless:check verify javadoc:jar

format:
	mvn spotless:apply

update-flake:
	-nix flake update

update-sysdig-cli-version:
	@echo "Fetching latest sysdig version…"
	@latest=$$(curl -sfSL https://download.sysdig.com/scanning/sysdig-cli-scanner/latest_version.txt) && \
	echo "Latest: $$latest" && \
	sed -i -E 's/(private static final String FIXED_SCANNED_VERSION = ")([^"]+)(")/\1'$$latest'\3/' src/main/java/com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/RemoteSysdigImageScanner.java && \
	echo "Version updated"

release:
	mvn release:prepare release:perform
