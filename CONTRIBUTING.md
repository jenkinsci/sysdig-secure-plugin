# Contributing to Sysdig Secure Jenkins Plugin

Thank you for your interest in contributing to the Sysdig Secure Jenkins Plugin! We appreciate your time and effort in improving this project. Please follow the guidelines below to ensure a smooth contribution process.

## Getting Started

1. **Fork the Repository**: Start by forking the [Sysdig Secure Jenkins Plugin](https://github.com/jenkinsci/sysdig-secure-plugin) repository.
2. **Clone Your Fork**:
   ```sh
   git clone https://github.com/YOUR_USERNAME/sysdig-secure-plugin.git
   cd sysdig-secure-plugin
   ```
3. **Set Up Upstream Remote**:
   ```sh
   git remote add upstream https://github.com/jenkinsci/sysdig-secure-plugin.git
   ```
4. **Create a Branch**:
   ```sh
   git checkout -b feature-branch-name
   ```

## Development Setup

This project uses **Maven** for build and dependency management. **Nix is recommended** but not required—it serves as a quality-of-life tool by providing a development environment with all necessary dependencies, including the correct Java version.

### Using Nix (Optional)

1. Ensure you have [Nix](https://determinate.systems/nix-installer/) installed.
2. Enter the development shell:
   ```sh
   nix develop
   ```
   This provides a development environment with the necessary tools (Maven, Java, etc.).
3. You can also build the plugin with:
   ```
   nix build
   ```
   The plugin will be compiled in `./result/sysdig-secure.hpi`&#x20;

### Using Maven

1. To build the plugin (it also runs the tests):
   ```sh
   mvn clean package
   ```
   The plugin will be compiled in `./target/sysdig-secure.hpi`&#x20;
2. To just run tests:
   ```sh
   mvn clean verify
   ```
3. You can also execute Jenkins with the plugin installed so you can test it locally with:
   ```
   mvn hpi:run
   ```

### Using the Makefile

The project includes a `Makefile` with useful commands:

- **Update dependencies and parent POM:**
  ```sh
  make update
  ```
- **Run verification tests:**
  ```sh
  make verify
  ```

## Making Changes

- The project follows the **Ports and Adapters (Hexagonal) Architecture**. Please ensure that your contributions respect this design pattern to maintain modularity and separation of concerns.

- Use **Conventional Commits** for commit messages. See [Conventional Commits](https://www.conventionalcommits.org/) for guidelines.

- Keep your changes focused on a single feature or fix.

- Follow the existing code style and best practices.

- Include meaningful commit messages.

- Write tests if applicable.

## Submitting a Pull Request

1. Push your branch to your fork:
   ```sh
   git push origin feature-branch-name
   ```
2. Open a Pull Request (PR) against the `main` branch in the upstream repository.
3. Ensure your PR description includes:
    - A summary of changes
    - Any related issue numbers (e.g., "Fixes #123")
    - Steps to test the changes
4. Wait for reviews and make necessary changes as requested.

## Code Review and Merging

- PRs require at least one approval from a maintainer.
- Automated tests must pass before merging.
- Squash commits if necessary to maintain a clean history.

## Reporting Issues

If you encounter any issues, please open a [GitHub Issue](https://github.com/jenkinsci/sysdig-secure-plugin/issues) with:

- A clear title and description
- Steps to reproduce the issue
- Logs or error messages if applicable

## License

By contributing, you agree that your contributions will be licensed under the [Apache 2.0 License](http://opensource.org/licenses/Apache-2.0).

---

Thank you for your contributions! 🚀

