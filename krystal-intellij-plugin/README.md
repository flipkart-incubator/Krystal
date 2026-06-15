# Krystal IntelliJ Plugin

IntelliJ IDEA support for authoring Krystal Vajrams.

## Features

- **Code generation actions** (Krystal menu / Generate):
  - New Vajram
  - Add Vajram Input
  - Add Dependency
  - Add Injection
  - Generate Output Method
  - Generate Resolver Method
- **Facet-aware autocomplete** while editing `@Output`, `@Resolve`, and `@Dependency` annotations

## Local development

The plugin builds against a **local IntelliJ installation** (required when JetBrains Maven repositories are unavailable).

```bash
# Default path: /Applications/IntelliJ IDEA.app/Contents
./gradlew :krystal-intellij-plugin:runIde

# Custom IDE path
./gradlew :krystal-intellij-plugin:runIde -Pkrystal.intellij.localPath="/path/to/IDE/Contents"
```

Build plugin distribution:

```bash
./gradlew :krystal-intellij-plugin:buildPlugin
```

## Requirements

- Java 21 toolchain (matches IntelliJ Platform 2025.3+)
- IntelliJ IDEA with the Java plugin
- Krystal libraries on the compile classpath (`vajram-codegen-common`, which transitively includes `vajram-java-sdk` and `krystal-common`)

## Generated source roots

For full autocomplete of `VajramId_Fac.*_n` and `VajramId_Req.*_n` constants, run `krystalModelsGen` in the target Gradle module so generated sources under `build/generated/sources/krystalModels/java/main` are indexed.
