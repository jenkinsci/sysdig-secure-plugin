# Help commands
default:
    @just --list

# Run all checks
check: format-check verify

# Verify project (clean build, tests, spotless check, javadoc)
verify:
    mvn clean spotless:check verify javadoc:jar

# Format code
format:
    mvn spotless:apply

# Check formatting without modifying
format-check:
    mvn spotless:check

# Run tests
test:
    mvn test

# Update everything: jenkins, parent pom, deps, flake, sysdig cli
update: update-jenkins-version update-parent-pom update-dependencies update-flake update-sysdig-cli-version

# Update to latest unmaintained LTS Jenkins version
update-jenkins-version:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "Fetching latest unmaintained LTS Jenkins version..."
    version=$(curl -s https://endoflife.date/api/v1/products/jenkins/ | jq -r '.result.releases | map(select(.isMaintained == false)) | first | .latest.name')
    baseline=$(echo "$version" | cut -d. -f1-2)
    echo "Found Jenkins version: $version (Baseline: $baseline)"
    echo "Fetching latest BOM version for bom-${baseline}.x..."
    bom_version=$(curl -s "https://repo.jenkins-ci.org/public/io/jenkins/tools/bom/bom-${baseline}.x/maven-metadata.xml" | grep -oPm1 "(?<=<latest>)[^<]+")
    echo "Found BOM version: $bom_version"
    sed -i "s|<jenkins.baseline>.*</jenkins.baseline>|<jenkins.baseline>${baseline}</jenkins.baseline>|" pom.xml
    sed -i "s|<jenkins.version>.*</jenkins.version>|<jenkins.version>${version}</jenkins.version>|" pom.xml
    sed -i "/<artifactId>bom-\${jenkins.baseline}.x<\/artifactId>/,+2 s|<version>.*</version>|<version>${bom_version}</version>|" pom.xml
    echo "Updated pom.xml to Jenkins $version, Baseline $baseline, BOM $bom_version"

# Update parent POM to latest version
update-parent-pom:
    mvn versions:update-parent -DgenerateBackupPoms=false

# Update dependencies to latest versions
update-dependencies:
    mvn versions:use-latest-versions

# Update nix flake
update-flake:
    -nix flake update

# Update sysdig CLI scanner version
update-sysdig-cli-version:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "Fetching latest sysdig version…"
    latest=$(curl -sfSL https://download.sysdig.com/scanning/sysdig-cli-scanner/latest_version.txt)
    echo "Latest: $latest"
    sed -i -E 's/(private static final String FIXED_SCANNED_VERSION = ")([^"]+)(")/\1'"$latest"'\3/' src/main/java/com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/RemoteSysdigImageScanner.java
    echo "Version updated"

# Run Jenkins locally with the plugin installed (http://localhost:8080/jenkins)
dev-run:
    mvn clean hpi:run

# Prepare and perform Maven release
release:
    mvn release:prepare release:perform
