# Repository Guidelines

## Project Purpose & Structure

The **Sysdig Secure Jenkins Plugin** integrates Sysdig Secure image scanning into Jenkins pipelines and freestyle jobs. It enables automated security scans and policy evaluations for container images.

### Module Organization
The codebase follows a **Hexagonal Architecture (Ports & Adapters)** to decouple the core domain from external dependencies (Jenkins, Sysdig API).

- **`src/main/java/com/sysdig/jenkins/plugins/sysdig/`**:
  - **`domain/`**: Core business logic (e.g., `ImageScanner`, `ScanResult`). Pure Java, no external dependencies.
  - **`application/`**: Use cases and orchestration (e.g., `ImageScanningService`).
  - **`infrastructure/`**: Implementations of ports (e.g., `jenkins/` for Jenkins integration, `scanner/` for Sysdig CLI).
- **`src/main/resources/`**: Jelly files for Jenkins UI (`index.jelly`) and other resources.
- **`src/test/java/`**: Unit and integration tests, mirroring the package structure.

## Build, Test, & Development

This project uses **Maven** for build management and optionally **Nix** for a consistent environment.

### Key Commands

- **Build Plugin**: `mvn clean package` (generates `target/sysdig-secure.hpi`).
- **Run Tests**: `mvn clean verify` (runs unit and integration tests).
- **Run Locally**: `mvn hpi:run` (starts a local Jenkins instance with the plugin installed).
- **Format Code**: `just format` (applies Spotless formatting).
- **Verify All**: `just verify` (runs formatting check + tests).
- **Dev Shell**: `nix develop` (enters a shell with Java/Maven pre-configured).

## Coding Style & Naming

- **Architecture**: Strictly adhere to Hexagonal Architecture. Do not leak Jenkins or Sysdig API dependencies into the `domain` package.
- **Formatting**:
  - **Java**: 4 spaces indentation.
  - **XML/JS**: 2 spaces indentation.
  - Enforced via **Spotless**. Always run `just format` before committing.
- **Naming**: Use descriptive, standard Java naming conventions (CamelCase for classes, camelCase for methods/variables).

## Testing Guidelines

- **Frameworks**: JUnit, Mockito.
- **Coverage**: Aim for high coverage in `domain` logic.
- **Conventions**:
  - Unit tests should mock external dependencies (ports).
  - Integration tests (in `infrastructure/` or `e2e/`) verify interactions with Jenkins or real APIs.
- **Execution**: Run specific tests via Maven or all tests with `mvn clean verify`.

## Commit & Pull Request Guidelines

### Commit Messages
Follow **Conventional Commits**:
- Format: `<type>(<scope>): <description>`
- Types: `feat`, `fix`, `refactor`, `chore`, `test`, `docs`.
- Examples:
  - `feat(ui): add image diff table`
  - `fix(scanner): respect accepted risks in policy evaluation`
  - `refactor(domain): split policy rules`

### Pull Requests
- Link relevant issues (e.g., "Fixes #149").
- Include a summary of changes.
- For UI changes, attach screenshots.
- Ensure `just verify` passes locally.

## Documentation Updates

The agent must keep this file (`AGENTS.md`) and `README.md` updated as the project evolves.
- If a new build tool or key command is added, update this guide.
- Suggest documentation improvements to the user if existing docs are unclear or outdated.

## Maintenance Guidelines

This project includes a `justfile` to simplify common maintenance tasks. **It is highly recommended to run `just update`** to perform a complete project update, as it orchestrates all maintenance tasks (Jenkins version, Parent POM, dependencies, etc.) in the correct order.

### Updating Dependencies & Parent POM
Regularly update the parent POM and project dependencies to ensure security and compatibility.

1.  **Update Parent POM**:
    Run `just update-parent-pom` (executes `mvn versions:update-parent`).
    *Reference: [Update parent POM](https://www.jenkins.io/doc/developer/tutorial-improve/update-parent-pom/)*

2.  **Update Dependencies**:
    Run `just update-dependencies` (executes `mvn versions:use-latest-versions`).

3.  **Update All (Recommended)**:
    Run `just update` to update the parent POM, dependencies, flake, and the Sysdig CLI version in one go.

### Updating Base Jenkins Version
**Important:** Update the base Jenkins version **before** updating the parent POM or other dependencies.

1.  **Run Automation**:
    Run `just update-jenkins-version`. This command:
    - Fetches the latest **unmaintained LTS version** from [endoflife.date](https://endoflife.date/jenkins).
    - Calculates the corresponding baseline (e.g., `2.516`).
    - Fetches the latest available **Bill of Materials (BOM)** version for that baseline.
    - Updates `jenkins.baseline`, `jenkins.version`, and the BOM dependency in `pom.xml` automatically using `sed`.

2.  **Verify**:
    Run `just verify` to ensure the plugin builds and tests pass with the new version.

### Updating Sysdig CLI Version
To update the embedded Sysdig CLI version used by the scanner:

1.  Run `just update-sysdig-cli-version`.
    This fetches the latest version from Sysdig and updates `RemoteSysdigImageScanner.java`.
