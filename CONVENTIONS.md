## Java Style

- All `if` statements must use curly braces, even for single-statement bodies (e.g. `if (...) { continue; }`, not `if (...) continue;`).

## JavaPoet Codegen Style

When generating multi-line code blocks in JavaPoet (codegen files):

- **Use text blocks** (`"""`) via `addCode("""...""", args...)` or
  `addNamedCode("""...""", Map.ofEntries(...))` — never `.beginControlFlow()` /
  `.endControlFlow()` / chained `.addStatement()` calls for multi-line logic.
- **Positional args** (`$T`, `$L`, `$S`): use `addCode("""...""", arg1, arg2, ...)` when each
  placeholder is used once.
- **Named args** (`$name:T`, `$name:L`, `$name:S`): use
  `addNamedCode("""...""", Map.ofEntries(...))` when the same value appears multiple times or
  embedded mid-identifier (e.g. `_$fieldName:L_offset`).
- Single-line `.addStatement(...)` calls are fine to keep as-is.
