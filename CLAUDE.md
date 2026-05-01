# Krystal Project Guidelines

## Publishing and Testing (CONTRIBUTING.md)

After any framework change, always follow this sequence:

1. **Publish locally** — run `upgradeVersionAndPublishToLocal.macOS.sh` so sample projects pick up the new framework version.
2. **Run all tests** — `./gradlew test --rerun-tasks -PunsafeCompile=true`
3. **Full build + analysis** — `./gradlew build -PunsafeCompile=true`

### Testing layers

- **Module-specific unit tests**: in `src/test` of each framework module — for locally testable, single-module features.
- **End2End unit tests**: in `*-sample` projects — for cross-module features. All new features should have coverage in both layers where possible.
