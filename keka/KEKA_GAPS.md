# Keka gaps found while building mitram

Goal: mitram drives keka's evolution. Add an entry whenever a need or bug surfaces; when fixed, move it to the "Fixed" table with the Krystal commit.

## Open

| ID | Type | Found in | Description | Workaround | Status |
|----|------|----------|-------------|-----------|--------|
| GAP-3 | language | all | No structs/entities (User, Post); everything is a string | Encode as strings | open |
| GAP-4 | runtime | all | No persistence; no way to create a post or friendship | Stubbed data | open |
| GAP-6 | runtime | n/a | No HTTP server entry point for vajrams | n/a | open |
| GAP-7 | runtime | n/a | `callHttp` is GET-only, no headers | n/a | open |

## Fixed

| ID | Type | Found in | Description | Fix |
|----|------|----------|-------------|-----|
| GAP-1 | compiler bug | `posts/getLatestPost.keka` | `facet + "literal"` lowers to `&String + &str` (Rust E0369) | `ExprEmitter` lowers a left-hand string facet with `.to_string()` (Krystal `1393bf29c`) |
| GAP-2 | diagnostics | `feed/getFeed.keka` | Async propagation is not inferred for the output type; the compiler errors and asks for `~` | Output completion is declared on `out`: ``now T``, `T~`/plain `T` (soon, the default) or `T~~`. A vajram can't complete earlier than its dependencies (Krystal `817992d7e`) |
| GAP-8 | runtime | `getFeed` (fanout) | A failure in one element of a vectorized call took down the whole batch (`.expect()` panics in `callHttp`/`readFileAsString`; `throw` outside `T?` didn't compile) | Errability is now `T?` or any `~`/`~~` vajram (`now` + `throw` is a compile error). Failures stay per item (a plain-`T` fanout fails an item only if all its elements fail) and auto-propagate to callers through plain-`T` facets; `callHttp`/`readFileAsString` yield per-item `Result`s (Krystal `799f567a4`) |
| GAP-5 | runtime | `showFeed` | `outsideProcess` CLI accepts only `string`/`int` inputs (no lists) | `List<string>`/`List<numeric>` inputs are passed as a repeated flag: `--friendIds bob --friendIds carol` (Krystal `799f567a4`). WASM dispatch still rejects lists. `getFeed` is now `outsideProcess`-callable |
