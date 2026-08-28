# Publishing and Testing Krystal

## tldr;

After any framework change, always follow this sequence:

1. **Publish locally** — run `upgradeVersionLocal.macOS.sh` so sample projects pick up the new framework version.
2. **Run all tests** — `./gradlew test --rerun -PunsafeCompile=true`
3. **Full build + analysis** — `./gradlew build -PunsafeCompile=true`

### Testing layers

- **Module-specific unit tests**: in `src/test` of each framework module — for locally testable, single-module features.
- **End2End unit tests**: in `*-sample` projects — for cross-module features. All new features should have coverage in both layers where possible.

## Testing in Krystal: Details

- Testing in krystal is done in two layers
    - Module specific unit tests
    - End2End unit tests
- Module specific unit tests are present in the src/test directory of each framework module
  and are designed to give immediate feedback for locally testable features. However not all
  features -
  especially ones which are implemented across modules - are testable in local unit tests.
- End2End unit tests are present in sample projects.
    - These tests are designed to test complex features whose implementation spans multiple modules
    - All sample projects' names end with the "-sample" suffix (and all projects whose names end
      in "-sample" are sample projects).
    - These sample projects contain various krystal graphs/workflows which are tested in end to end
      fashion in the src/test of that sample project.
    - These sample projects also act as documentation to help clients learn how to use krystal and
      its various modules and extensions.
- All features implemented should preferably have test cases in both module specific unit tests and
  End2End unit tests.

### IntelliJ Plugin (Krystal Vajrams) (WIP)

The `krystal-intellij-plugin` module provides IDE actions and facet-aware autocomplete for authoring Vajrams.

```bash
./gradlew :krystal-intellij-plugin:runIde
```

See [`krystal-intellij-plugin/README.md`](krystal-intellij-plugin/README.md) for setup details (local IntelliJ path, Java 21 toolchain)