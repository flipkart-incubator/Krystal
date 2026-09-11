## Code Invariants

* Generated code or code in application execution paths (hot paths) should **NEVER** use reflection.
  That is against the philosophy of Krystal. If you encounter a scenario where reflection seems
  mandatory, raise an issue, or ask for feedback before going ahead and implementing the change.

## Java Style Invariants

- All `if` statements must use curly braces, even for single-statement bodies (e.g.
  `if (...) { continue; }`, not `if (...) continue;`).

## JavaPoet Codegen Style Invariants

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
- **Use class objects instead of canonical Strings**: If the class being referred to in a code
  generator/annotation processor is part of the classpath, then just use T.class instead of using "
  a.b.c.T" to refer the class in java-poet objects like ClassName.This makes finding usages and
  code refactorings like class renaming easier. 
