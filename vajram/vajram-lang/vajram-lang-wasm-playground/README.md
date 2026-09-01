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

## How It Works

1. The browser sends the editor's virtual `.vajram` files to the local compiler service.
2. The service compiles them for `wasm32-unknown-unknown`, builds a disposable `cdylib`, and runs
   `wasm-bindgen` to create browser JavaScript glue and a WASM artifact.
3. The browser imports the generated glue and invokes the selected `outsideProcess` Vajram export.
   Native CLI `main.rs` is not used in the browser target.
4. Before WASM initialization, the page installs the `emit_vajram_output` host callback. The WASM
   runtime's injected `ConsoleWriter` calls this callback, so `println` messages are collected and
   displayed in the execution output before the Vajram return value.
