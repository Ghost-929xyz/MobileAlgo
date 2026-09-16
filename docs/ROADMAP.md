# Roadmap

## M0 - Repository foundation

- [x] Android/Compose project skeleton
- [x] Workspace and editor state
- [x] CPH JSON model and repository
- [x] Run plan and command builders
- [x] DAP frame codec
- [x] Runtime manifest and installer flow
- [ ] Build in CI with SDK 35

## M1 - End-to-end local execution

- [ ] Pin Termux bootstrap artifacts and SHA-256
- [ ] Extract bootstrap with symlink/Unix mode support
- [ ] Verify Python, clang++ and javac on arm64 device
- [ ] Stream process output into Compose console
- [ ] Add process timeout and cancellation
- [ ] Add SAF import/export

## M2 - Competitive programming workflow

- [ ] CPH import from clipboard/file picker
- [ ] Expected/actual diff viewer
- [ ] Per-test timing and memory
- [ ] Problem statement quick link
- [ ] Stress test generator and reference solution
- [ ] Share testcase set as a file

## M3 - Debugging

- [ ] Python debugpy adapter
- [ ] C/C++ lldb-dap adapter
- [ ] Java JDI bridge
- [ ] Breakpoint persistence
- [ ] Stack/variables bottom sheet
- [ ] Conditional breakpoints and logpoints

## M4 - Store-compatible runtime

- [ ] Embedded Python provider
- [ ] In-process Java compiler + DEX runner
- [ ] C/C++ compile service contract
- [ ] targetSdk 35+
- [ ] Google Play policy review
