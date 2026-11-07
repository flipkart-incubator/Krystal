---
title: "Keka Samples"
---

# Keka Samples

This module compiles `.keka` sources in `src/main/keka` with `keka-rust-compiler` and packages the generated code as a Cargo crate under `build/generated-rust`.
It targets Rust 2024 and requires Rust/Cargo 1.85 or newer to run samples.

Compile all samples:

```bash
./gradlew :keka:keka-samples:compileVajram
```

Compile and run the default `helloWorld` sample through the generated `outsideProcess` CLI:

```bash
./gradlew :keka:keka-samples:runVajram
```

Select another public `outsideProcess` Vajram with `-Pvajram`:

```bash
./gradlew :keka:keka-samples:runVajram -Pvajram=helloWorld2
```

Pass named Vajram inputs with `-PvajramArgs`:

```bash
./gradlew :keka:keka-samples:runVajram -Pvajram=headFile -PvajramArgs="--numChars 5 --filePath /path/to/file"
```

Cargo integration tests under `src/test/rust/tests` invoke every sample Vajram through the generated CLI. Run them with:

```bash
./gradlew :keka:keka-samples:test
```

To add another runnable sample, add a no-input Vajram with the following callers declaration; no hand-written main method is required:

```vajram
permit callers `outsideProcess public
```
