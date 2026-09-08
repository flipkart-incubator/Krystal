# Vajram-Lang Samples

This module compiles `.vajram` sources in `src/main/vajram` with `vajram-lang-rust-compiler` and packages the generated code as a Cargo crate under `build/generated-rust`.
It targets Rust 2024 and requires Rust/Cargo 1.85 or newer to run samples.

Compile all samples:

```bash
./gradlew :vajram:vajram-lang:vajram-lang-samples:compileVajram
```

Compile and run the default `helloWorld` sample through the generated `outsideProcess` CLI:

```bash
./gradlew :vajram:vajram-lang:vajram-lang-samples:runVajram
```

Select another public `outsideProcess` Vajram with `-Pvajram`:

```bash
./gradlew :vajram:vajram-lang:vajram-lang-samples:runVajram -Pvajram=helloWorld2
```

Pass positional Vajram inputs with `-PvajramArgs`:

```bash
./gradlew :vajram:vajram-lang:vajram-lang-samples:runVajram -Pvajram=headFile -PvajramArgs="5 /path/to/file"
```

Cargo integration tests under `src/test/rust/tests` invoke every sample Vajram through the generated CLI. Run them with:

```bash
./gradlew :vajram:vajram-lang:vajram-lang-samples:test
```

To add another runnable sample, add a no-input Vajram with the following callers declaration; no hand-written main method is required:

```vajram
permit callers `outsideProcess public
```
