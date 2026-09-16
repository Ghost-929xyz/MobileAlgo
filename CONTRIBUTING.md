# Contributing

AlgoForge is an Android-first, offline-capable algorithm IDE. Contributions are welcome, especially around runtime provisioning, mobile editing UX, and debug adapters.

## Development setup

1. Install Android Studio Ladybug or newer.
2. Install Android SDK 35, Build Tools 35.0.0, Platform Tools, and NDK 27.
3. Use JDK 17.
4. Run `./gradlew :app:assembleDebug`.

Runtime binaries are not committed. See `docs/runtime.md` for the download/update flow.

## Pull requests

- Keep domain logic in `:core` free from Android APIs.
- Add tests for command construction, CPH parsing, and DAP framing changes.
- Do not commit downloaded toolchains, APKs, signing keys, or user workspaces.
- Document any runtime or license change.
