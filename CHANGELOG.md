# Changelog

All notable changes to AlgoForge are documented here.

## [0.1.0] - Unreleased

### Added

- Android/Compose IDE shell with code, tests, console, and environment workspaces.
- Python, C, C++, and Java project templates.
- Pure-Kotlin run-plan model and command construction.
- CPH-compatible `.cph/*.json` codec, repository, and output matcher.
- PRoot/Termux runtime provider with HTTPS download, pinned size, and SHA-256 verification.
- Runtime ABI resolution, safer tar extraction, atomic file replacement, process timeout, and cancellation.
- DAP frame codec and adapter discovery scaffolding.
- Core and runtime unit tests for run plans, CPH, DAP, atomic writes, runtime resolution, and guest paths.
- Android CI workflow and repository support files.

### Known limitations

- Runtime artifact URLs and hashes are placeholders; installation fails closed until release artifacts are pinned.
- Python, C/C++, and Java toolchains are installed after the bootstrap rather than bundled in the APK.
- Live debug sessions, SAF import/export, and store-compatible embedded runtimes are not implemented yet.
- No APK has been built or verified on a physical Android device in the development environment.