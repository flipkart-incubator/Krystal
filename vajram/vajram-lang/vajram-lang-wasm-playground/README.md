# Vajram WASM Playground

Build the standalone distribution:

```bash
./gradlew :vajram:vajram-lang:vajram-lang-wasm-playground:playgroundZip
```

Unpack the ZIP, run `bin/vajram-lang-wasm-playground --check`, then run
`bin/vajram-lang-wasm-playground`. The server prints its localhost URL.

The distribution bundles the compiler, browser UI, language specification, and samples. It does
not require a Krystal repository checkout or Gradle after unpacking. Rust/Cargo,
`wasm32-unknown-unknown`, and `wasm-bindgen` remain local prerequisites.

The compiler service receives virtual editor files, builds only WASM artifacts in disposable temp
directories, and never invokes authored Vajrams. Generated artifacts are loaded and invoked by the
browser page.
