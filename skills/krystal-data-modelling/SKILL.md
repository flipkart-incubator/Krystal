---
name: krystal-data-modelling
description: Defines structured data models — request/response payloads, nested value objects, and enums — as Krystal `@ModelRoot` interfaces/enums (`com.flipkart.krystal.model.**` in krystal-common) in the Krystal framework (flipkart-incubator/Krystal). Covers `@ModelRoot` (type=REQUEST/RESPONSE, pure, builderExtendsModelRoot), field annotations (`@SerialId`, `@IfAbsent`, `@Nullable`/`Optional`), `@SupportedModelProtocol` (PlainJavaObject/Json/Protobuf3/Fory) and the generated `_Immut`/`_ImmutPojo`/`_ImmutJson`/`_ImmutProto`/`_ImmutFory` builders, enum model roots (`EnumModel`, `UNKNOWN`-first rule), model purity, and nested-model/collection constraints. Use whenever the user wants to define/change a data model, DTO, request or response shape, a nested/enum field type, or asks "what protocols/serde does this model support" in a Krystal-based repo — even if they just say "add a field to X" or "model this payload" without naming `@ModelRoot`. Not for Vajram business logic (`_Inputs`/`_InternalFacets`/`@Dependency`/`@Resolve`/`@Output` — use vajram-authoring) and not for SQL `@Table` schemas (use vajram-sql-data-modelling) — both of those build on top of the models this skill defines.
---

# Modelling data with Krystal `@ModelRoot`

A Krystal model is a plain Java `interface` (or `enum`) annotated `@ModelRoot` that declares its fields as
zero-argument methods. The Krystal annotation processor reads this declaration at compile time and generates an
immutable implementation, a builder, and (optionally) serde wrappers for JSON/Protobuf/Fory — you never
hand-write any of that generated code. This is the modelling layer other Krystal constructs build on: a
Vajram's `_Inputs`/output type, a REST/gRPC payload, a `@Table` row — are all, underneath, `@ModelRoot` types.

## Step 1 — Find how this codebase already does this

Every model-heavy repo settles into its own conventions on top of the framework's rules:
- Search this repo for existing `@ModelRoot` interfaces near the feature you're adding (same module, or the
  Vajram/service that will use this model) to match its package layout (`.../model/`, `.../models/` are both
  common) and naming style.
- Check a sibling module's build file for which serde codegen processors are already wired (Step 10) — new
  protocol support on an existing model, or a brand-new module, may need more than what's already there.
- If anything below is ambiguous, the framework's own source in
  [flipkart-incubator/Krystal](https://github.com/flipkart-incubator/Krystal) (`krystal-common`'s
  `com.flipkart.krystal.model` / `com.flipkart.krystal.serial` packages) is the ground truth — this skill's
  `references/` files are already verified against it, including a couple of places where the framework's own
  `Krystal-models.md` doc has drifted from the real annotation shape (see `references/annotations.md`).

## Step 2 — Decide if this needs to be a `@ModelRoot` at all

Reach for `@ModelRoot` when the data:
- crosses a serialization boundary (REST/gRPC payload, a Vajram's `_Inputs`/output, a message on a queue), or
- is a nested value object referenced by a field of another `@ModelRoot`, or
- is an enum that needs to round-trip through one of the above.

For a purely internal, in-process value with no serde/boundary need (a local computation's intermediate
result, say), a plain Java `record` is simpler and doesn't need codegen.

## Step 3 — Deliberate on the design before writing any field

Several `@ModelRoot` parameters aren't just codegen switches — they shape what the generated code looks like,
which protocols the model can ever support, and how safely it can evolve later. Several are effectively
**one-way doors**: reversing them once other code depends on the model is a breaking change, not a quick edit.
Don't silently default these — read `references/design-questions.md` and work through whichever of these
apply, explaining each option's meaning and trade-offs to the user (or stating your recommended default and why,
if you're confident, so it can be corrected) rather than picking one unprompted:

1. Does this really need to be a *new* `@ModelRoot` (vs. reusing an existing one, or a plain `record`)?
2. What is this model's role — `type` = `REQUEST`, `RESPONSE`, both, or general-purpose?
3. Should it be `pure`, or does it need an escape hatch for a non-Krystal type (`pure = false`)?
4. Which wire protocols does it actually need (`@SupportedModelProtocol`) — just `PlainJavaObject`, or also
   `Json`/`Protobuf3`/`Fory`?
5. Does it need stable binary field indices now (`@SerialId`), or can it rely on declaration order?
6. Should the builder double as the model type (`builderExtendsModelRoot`)?
7. Will other modules/repos build on this model (`isShared`)?
8. What's the mandatory-vs-optional stance per field (`@IfAbsent`) — and, for an enum model root, which
   constant absorbs unknown/missing values?

Only once these are answered should you move on to Steps 4-10, which turn the answers into the actual
interface/enum declaration.

## Step 4 — Pick interface vs. enum shape

- **Interface model root** — `interface Foo extends Model`, fields as no-arg methods. Use for structured
  data with multiple fields.
- **Enum model root** — `enum Foo implements EnumModel`, one constant per value, `UNKNOWN` always first. Use
  for a closed, named set of values used as a field type elsewhere.

Both are annotated `@ModelRoot`; read `references/annotations.md` before writing either — the enum shape has
extra constraints (`UNKNOWN`-first, `@SerialId(0)` on it) that aren't obvious from the interface case.

## Step 5 — Set `@ModelRoot`'s `type`

- **Empty (default, `{}`)** — general-purpose model, no request/response-specific rules. Can be nested inside
  any other model.
- **`REQUEST`** — accepts data from a client. Can only be nested inside other `REQUEST` (or dual-type) models.
- **`RESPONSE`** — returned to a client. Can only be nested inside other `RESPONSE` (or dual-type) models.
- **`{REQUEST, RESPONSE}`** — serves both; must satisfy both rule sets, and every field needs an *explicit*
  `@IfAbsent` (no default applies).

Getting this wrong surfaces as a compile error the moment you nest the model somewhere its `type` doesn't
allow — see `references/annotations.md`'s "Nested Model Type Consistency" section for the exact rule and
examples.

## Step 6 — Declare fields and their `@IfAbsent` strategy

One no-arg method per field (abstract or default; default methods are ignored by codegen/serde). Every field
needs a deliberate `@IfAbsent(...)` choice — `FAIL` (mandatory), `WILL_NEVER_FAIL` (optional, type must be
`@Nullable T`/`Optional<T>`), `MAY_FAIL_CONDITIONALLY` (conditionally mandatory — request-side only, needs
`conditionalFailureInfo`), or `ASSUME_DEFAULT_VALUE` (falls back to a zero-like default). The *implicit*
default when you omit the annotation differs by `type` (`FAIL` for RESPONSE/general-purpose,
`MAY_FAIL_CONDITIONALLY` for REQUEST-only) — don't rely on the implicit default without checking which one
applies; see `references/annotations.md` for the full table. Don't use array return types (`List<T>` instead)
and don't prefix method names with `get`.

## Step 7 — `@SerialId` — all-or-none

If any field needs a stable binary index (for Protobuf/Fory-style protocols), put `@SerialId(n)` on *every*
field of that model — partial usage is a compile error. Skip it entirely and codegen falls back to declaration
order. Enum constants follow the same all-or-none rule, with `UNKNOWN` always `@SerialId(0)`.

## Step 8 — Declare `@SupportedModelProtocol`

One annotation per protocol — `value()` takes a single `Class`, not an array. Both `Krystal-models.md`
and every framework sample use this repeated form (see `references/annotations.md` for the full
`@interface` definition):

```java
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Json.class)
```

- Omit entirely → only the plain `_Immut` interface is generated (no POJO, no serde).
- `PlainJavaObject` → generates `_ImmutPojo` (in-memory POJO), during the same compiler phase as the model
  itself.
- `Json` / `Protobuf3` / `Fory` → generate `_ImmutJson` / `_ImmutProto` / `_ImmutFory` serde wrappers in a later
  phase, each requiring its own codegen module on the annotation-processor classpath (Step 10).
- A nested model field must support at least the same protocols as its parent — compile-time checked.
- `Protobuf3` (and `Fory`) require the model to be pure (`@ModelRoot(pure = true)`, the default) — a compile
  error otherwise.

## Step 9 — Purity, nesting, and collection constraints

- **Purity** (`@ModelRoot(pure = true)`, the default): fields restricted to primitives/boxed primitives,
  `String`, `PrimitiveArray` subtypes (e.g. `ByteArray`), other pure `@ModelRoot` models/enums, and
  `List`/`Map` of these. Set `pure = false` only when a field genuinely needs a type outside this closed set
  (a third-party DTO) — and don't pair it with a protocol that requires purity.
- **No nested collections** — `List<List<T>>`, `Map<K, List<V>>`, etc. are all compile errors, regardless of
  purity/protocol. Wrap the inner collection in its own `@ModelRoot` model instead.
- **Map keys**: primitives/boxed/`String`/`@ModelRoot` enum only in pure models; enum keys are additionally
  disallowed once *any* `SerdeProtocol` (`Json`/`Protobuf3`/`Fory`) is supported (unknown values can't
  round-trip reliably); Protobuf3 further restricts keys to integral types and `String`.

Full worked examples of each constraint are in `references/examples.md`.

## Step 10 — Wire the build (new module, or new protocol on an existing one)

The base MODELS-phase generator (`_Immut`, `_Immut.Builder`, `_ImmutPojo`) comes from the same
`vajram-codegen` processor used for Vajrams — if this module already builds Vajrams, it's already wired. Each
serde protocol beyond `PlainJavaObject` additionally needs its own codegen module registered on
`krystalModelsGenProcessor` (Gradle) — e.g. `vajram-json-codegen` for `Json`, `vajram-protobuf3-codegen` for
`Protobuf3`, `vajram-fory-codegen` for `Fory`. Match however this repo already declares Krystal dependencies
(version-catalog aliases vs. raw coordinates vs. `project(...)`) rather than inventing a new form — see
`references/annotations.md`'s build-wiring note for a real example. A bare `@ModelRoot` with no
`@SupportedModelProtocol` still generates `_Immut`/`_Immut.Builder`; nothing beyond that appears in
`build/generated` until you add a protocol.

## Step 11 — Use the generated types

Construct via `<Model>_ImmutPojo._builder()` (or `_ImmutJson`/`_ImmutProto`/`_ImmutFory` if you want that
protocol's wrapper directly), chain field setters, call `._build()`. For a nested model field, you can pass
either the built instance or the nested model's own `Builder` directly (codegen overloads the setter for
both). Round-trip serde is `instance._serialize()` → `byte[]`, and `new <Model>_ImmutJson(bytes)` (or the
`_ImmutProto`/`_ImmutFory` equivalent) to deserialize. `_asBuilder()` turns a built immutable instance back
into a builder; `_newCopy()` deep-copies. See `references/examples.md` for real construct/serialize/deserialize
snippets pulled from this framework's own sample/test code.

## Step 12 — Sanity-check before calling it done

- Every field has a deliberate `@IfAbsent(...)`, not whatever the implicit default for this model's `type`
  happens to be.
- `@SerialId` is on every field or none (same for enum constants, plus `UNKNOWN = @SerialId(0)`).
- `@SupportedModelProtocol` is declared once per protocol (not array-valued), and every nested model field
  supports at least the same set.
- No nested `List`/`Map` inside another `List`/`Map` — wrapped in an intermediate model if needed.
- If `type` is `REQUEST`- or `RESPONSE`-only, every nested model's `type` includes that same value (or is
  general-purpose).
- If you added a new protocol, the matching codegen module is on `krystalModelsGenProcessor` (Step 10) —
  otherwise the build silently only produces `_Immut`/`_ImmutPojo` with no error telling you why the serde
  wrapper is missing.

## Reference files

- `references/design-questions.md` — the full pros/cons and doc references for every design question in Step
  3, one section per question — read this *before* writing any model, and use it to actually ask the user
  rather than silently defaulting.
- `references/annotations.md` — full annotation reference (`@ModelRoot`, `@SerialId`, `@IfAbsent`,
  `@SupportedModelProtocol`, `EnumModel`, `@DefaultValue`, `@DefaultSerdeProtocol`, `PrimitiveArray` types), the
  generated-artifact table, and documented doc/source drift — read this before writing any model.
- `references/examples.md` — real, worked examples drawn from this framework's own sample modules
  (JSON/Protobuf3/Fory request & response models, nested models, `builderExtendsModelRoot`, enum model roots)
  plus construct/serialize/deserialize usage code, all linked to their real location in
  [flipkart-incubator/Krystal](https://github.com/flipkart-incubator/Krystal) on GitHub (this repo won't
  necessarily be checked out alongside whatever repo you're modelling data in).
