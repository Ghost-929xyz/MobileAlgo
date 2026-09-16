# Security Policy

## Supported versions

AlgoForge is pre-release software. Security fixes are applied to the latest `main` branch until a stable release line exists.

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability. Use GitHub's private security advisory flow for the published repository:

`Security` -> `Advisories` -> `Report a vulnerability`

Include the affected version or commit, reproduction steps, impact, and any suggested mitigation. Do not attach real credentials, private source code, or user workspaces.

## Runtime artifact policy

Runtime and toolchain downloads are treated as executable supply-chain inputs:

- artifact URLs must use HTTPS;
- size and SHA-256 must be pinned in the runtime manifest;
- redirects to non-HTTPS endpoints are rejected;
- unpinned placeholders must fail closed;
- release assets must have a documented source and license.

The app must never silently execute an artifact whose checksum has not been verified.