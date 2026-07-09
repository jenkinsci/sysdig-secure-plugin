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
update: update-jenkins-version update-parent-pom update-dependencies update-flake update-cli-scanner update-oldest-cli-scanner generate-scanner-fixtures

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

# (internal) Print the latest published sysdig-cli-scanner version
[private]
[group('scanner')]
_latest-version:
    @curl --silent --fail --show-error --location https://download.sysdig.com/scanning/sysdig-cli-scanner/latest_version.txt | tr -d '[:space:]'

# Find the oldest sysdig-cli-scanner version still within the support window (default 365 days)
[group('scanner')]
oldest-cli-scanner window_days="365":
    #!/usr/bin/env bash
    set -euo pipefail
    base="https://download.sysdig.com/scanning/bin/sysdig-cli-scanner"
    os="linux"; arch="amd64"
    cutoff=$(( $(date -u +%s) - {{window_days}} * 86400 ))
    latest=$(just _latest-version)
    major=${latest%%.*}
    minor=$(echo "$latest" | cut -d. -f2)
    oldest_ver=""; oldest_epoch=""
    for m in $(seq "$minor" -1 0); do
        minor_hit=0; misses=0
        for p in $(seq 0 30); do
            v="$major.$m.$p"
            lm=$(curl -sfI "$base/$v/$os/$arch/sysdig-cli-scanner" \
                | grep -i '^last-modified:' | sed 's/^[Ll]ast-[Mm]odified: //' | tr -d '\r' || true)
            if [ -z "$lm" ]; then
                misses=$((misses + 1)); [ "$misses" -ge 2 ] && break; continue
            fi
            misses=0
            epoch=$(date -u -d "$lm" +%s)
            if [ "$epoch" -ge "$cutoff" ]; then
                minor_hit=1
                if [ -z "$oldest_epoch" ] || [ "$epoch" -lt "$oldest_epoch" ]; then
                    oldest_epoch=$epoch; oldest_ver=$v
                fi
            fi
        done
        # Versions are chronological: once a whole minor is out of window, stop.
        [ "$minor_hit" -eq 0 ] && [ -n "$oldest_ver" ] && break
    done
    if [ -z "$oldest_ver" ]; then
        echo "No version found within the last {{window_days}} days" >&2
        exit 1
    fi
    echo >&2 "Oldest supported: $oldest_ver (released $(date -u -d "@$oldest_epoch" '+%Y-%m-%d'))"
    echo "$oldest_ver"

# (internal) Replace the version tagged with <marker>-version-marker wherever it
# appears. Markers are HTML-comment spans in Markdown and trailing `#`/`//`
# comments in Java/YAML. Target files are discovered, not hardcoded, so a new
# marker anywhere is picked up automatically. DO NOT delete those markers.
[private]
[group('scanner')]
_set-version marker version:
    #!/usr/bin/env bash
    set -euo pipefail
    # Discover files carrying this marker. Skip build output (target/work), the
    # git dir, and the tooling/docs that only name the marker in prose.
    mapfile -t files < <(grep -rl \
        --exclude-dir=.git --exclude-dir=target --exclude-dir=work \
        --exclude=justfile --exclude=AGENTS.md --exclude=CONTRIBUTING.md \
        "{{marker}}-version-marker" . | sort)
    if [ "${#files[@]}" -eq 0 ]; then
        echo "No files found carrying {{marker}}-version-marker" >&2
        exit 1
    fi
    for f in "${files[@]}"; do
        echo "Updating $f" >&2
        # Markdown: <!-- {{marker}}-version-marker ... -->X<!-- /{{marker}}-version-marker -->
        sed -i -E "s#(<!-- {{marker}}-version-marker[^>]*-->)[0-9][0-9.]*(<!-- /{{marker}}-version-marker -->)#\1{{version}}\2#g" "$f"
        # Java/YAML: line carrying a `#`/`//` {{marker}}-version-marker comment
        sed -i -E "/(#|\/\/)[[:space:]]*{{marker}}-version-marker/ s/[0-9]+\.[0-9]+\.[0-9]+/{{version}}/" "$f"
    done

# Substitute the oldest supported version wherever the oldest-version-marker is placed
[group('scanner')]
update-oldest-cli-scanner window_days="365":
    #!/usr/bin/env bash
    set -euo pipefail
    oldest=$(just oldest-cli-scanner {{window_days}})
    just _set-version oldest "$oldest"
    echo "Oldest supported version set to $oldest (via oldest-version-marker)"

# Update sysdig-cli-scanner default to the latest available version
[group('scanner')]
update-cli-scanner:
    #!/usr/bin/env bash
    set -euo pipefail
    latest=$(just _latest-version)
    just _set-version newest "$latest"
    echo "Newest (default) version set to $latest (via newest-version-marker)"

# (internal) Record raw scanner output for a specific version into a gzipped test fixture.
# SECURE_API_TOKEN is mandatory (the `env_var` default aborts if unset) and, being unused as
# {{_token}}, is never interpolated into the script; the scanner reads it from the environment.
# The endpoint comes from SECURE_API_URL (default https://secure.sysdig.com).
[private]
[group('scanner')]
generate-scanner-fixture version out image="debian:11.4-slim" api_url=env_var_or_default("SECURE_API_URL", "https://secure.sysdig.com") _token=env_var("SECURE_API_TOKEN"):
    #!/usr/bin/env bash
    set -euo pipefail
    case "$(uname -s)" in Linux) os=linux;; Darwin) os=darwin;; *) echo "unsupported OS" >&2; exit 1;; esac
    case "$(uname -m)" in x86_64|amd64) arch=amd64;; arm64|aarch64) arch=arm64;; *) echo "unsupported arch" >&2; exit 1;; esac
    workdir=$(mktemp -d)
    trap 'rm -rf "$workdir"' EXIT
    bin="$workdir/sysdig-cli-scanner"
    echo "Downloading sysdig-cli-scanner {{version}} ($os/$arch)…" >&2
    curl -sfL "https://download.sysdig.com/scanning/bin/sysdig-cli-scanner/{{version}}/$os/$arch/sysdig-cli-scanner" -o "$bin"
    chmod +x "$bin"
    json="$workdir/scan-result.json"
    echo "Scanning {{image}} with {{version}}…" >&2
    # The scanner exits non-zero when the policy evaluation fails; the JSON is still produced.
    set +e
    "$bin" --apiurl="{{api_url}}" --output=json-file="$json" --loglevel=info "{{image}}"
    rc=$?
    set -e
    if [ ! -s "$json" ]; then
        echo "scanner produced no output (exit $rc)" >&2
        exit 1
    fi
    mkdir -p "$(dirname "{{out}}")"
    gzip -c "$json" > "{{out}}"
    echo "Wrote fixture {{out}} (scanner {{version}}, exit $rc)" >&2

# Regenerate the newest and oldest test fixtures for the current testing window.
# SECURE_API_TOKEN is mandatory (the `env_var` default aborts before running if unset) and
# is never interpolated ({{_token}} is unused), so the secret never lands in the recipe
# script; the scanner reads it straight from the inherited environment. The Sysdig Secure
# endpoint is configurable via SECURE_API_URL (defaults to https://secure.sysdig.com).
[group('scanner')]
generate-scanner-fixtures image="debian:11.4-slim" api_url=env_var_or_default("SECURE_API_URL", "https://secure.sysdig.com") _token=env_var("SECURE_API_TOKEN"):
    #!/usr/bin/env bash
    set -euo pipefail
    dir="src/test/resources/com/sysdig/jenkins/plugins/sysdig/infrastructure/scanner/report/v1"
    newest=$(just _latest-version)
    oldest=$(just oldest-cli-scanner)
    just generate-scanner-fixture "$newest" "$dir/scanner_newest_scan_result.json.gz" "{{image}}" "{{api_url}}"
    just generate-scanner-fixture "$oldest" "$dir/scanner_oldest_scan_result.json.gz" "{{image}}" "{{api_url}}"
    echo "Fixtures regenerated (newest=$newest, oldest=$oldest, apiurl={{api_url}}). Also run 'just update-cli-scanner' and 'just update-oldest-cli-scanner' to sync the recorded versions."
