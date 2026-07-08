# Help commands
[private]
default:
    @just --list

# Run all checks
[group('build')]
check: format-check verify

# Verify project (clean build, tests, spotless check, javadoc)
[group('build')]
verify:
    mvn clean spotless:check verify javadoc:jar

# Run tests
[group('build')]
test:
    mvn test

# Format code
[group('format')]
format:
    mvn spotless:apply

# Check formatting without modifying
[group('format')]
format-check:
    mvn spotless:check

# Run Jenkins locally with the plugin installed (http://localhost:8080/jenkins)
[group('dev')]
dev-run:
    mvn clean hpi:run

# Prepare and perform Maven release
[group('dev')]
[confirm('This will prepare and perform a Maven release. Continue?')]
release: format-check
    mvn release:prepare release:perform

# Update everything: jenkins, parent pom, deps, flake, sysdig cli
[group('update')]
update: update-jenkins-version update-parent-pom update-dependencies update-flake update-sysdig-cli-version

# Update to latest unmaintained LTS Jenkins version
[group('update')]
update-jenkins-version:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "Fetching latest unmaintained LTS Jenkins version..."
    version=$(curl --silent https://endoflife.date/api/v1/products/jenkins/ | jq -r '.result.releases | map(select(.isMaintained == false)) | first | .latest.name')
    baseline=$(echo "$version" | cut --delimiter=. --fields=1-2)
    echo "Found Jenkins version: $version (Baseline: $baseline)"
    echo "Fetching latest BOM version for bom-${baseline}.x..."
    bom_version=$(curl --silent "https://repo.jenkins-ci.org/public/io/jenkins/tools/bom/bom-${baseline}.x/maven-metadata.xml" | grep --only-matching --perl-regexp --max-count=1 "(?<=<latest>)[^<]+")
    echo "Found BOM version: $bom_version"
    sed --in-place "s|<jenkins.baseline>.*</jenkins.baseline>|<jenkins.baseline>${baseline}</jenkins.baseline>|" pom.xml
    sed --in-place "s|<jenkins.version>.*</jenkins.version>|<jenkins.version>${version}</jenkins.version>|" pom.xml
    sed --in-place "/<artifactId>bom-\${jenkins.baseline}.x<\/artifactId>/,+2 s|<version>.*</version>|<version>${bom_version}</version>|" pom.xml
    echo "Updated pom.xml to Jenkins $version, Baseline $baseline, BOM $bom_version"

# Update parent POM to latest version
[group('update')]
update-parent-pom:
    mvn versions:update-parent -DgenerateBackupPoms=false

# Update dependencies to latest versions
[group('update')]
update-dependencies:
    mvn versions:use-latest-versions

# Update nix flake
[group('update')]
update-flake:
    -nix flake update

# Update sysdig CLI scanner version
[group('update')]
update-sysdig-cli-version:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "Fetching latest sysdig version…"
    latest=$(curl --silent --fail --show-error --location https://download.sysdig.com/scanning/sysdig-cli-scanner/latest_version.txt)
    echo "Latest: $latest"
    sed --in-place --regexp-extended 's/(private static final String FIXED_SCANNED_VERSION = ")([^"]+)(")/\1'"$latest"'\3/' src/main/java/com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/ScannerVersionResolver.java
    echo "Version updated"
