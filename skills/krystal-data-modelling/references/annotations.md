# Krystal `@ModelRoot` annotation reference

Source of truth: `com.flipkart.krystal.model` and `com.flipkart.krystal.serial` packages in `krystal-common`, in
[flipkart-incubator/Krystal](https://github.com/flipkart-incubator/Krystal). That framework repo is a separate
codebase from wherever you're modelling data — you won't find these files locally unless you're working inside
Krystal itself. The framework ships its own prose doc, `krystal-common/Krystal-models.md`; this reference folds
in that doc's content plus corrections verified directly against the real `.java` source and real compiling
samples — trust this file over the prose doc where they disagree (flagged explicitly below).

## The shape of an interface model root

```java
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Protobuf3.class)
public interface OrderResponse extends Model {

  @SerialId(1)
  @IfAbsent(FAIL)
  String orderId();

  @SerialId(2)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable String trackingUrl();

  @SerialId(3)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<LineItem> items();
}
```

Structural rules, enforced at compile time by the annotation processor:
- Must `extends Model` (`com.flipkart.krystal.model.Model`).
- Every non-static, non-underscore-prefixed method (including inherited ones) is a field declaration. Names
  starting with `_` are reserved for platform-generated methods (`_build`, `_builder`, `_serialize`, ...) — never
  declare a field method starting with `_`.
- Field methods can be abstract or `default`; **`default`-method fields are ignored by codegen and serde
  protocols** — only use a `default` method for a field you deliberately want excluded from generation (mirrors
  the same idiom vajram-sql uses to exclude DB-assigned columns from `INSERT`).
- Abstract field methods: zero parameters, non-`void` return type.
- No array return types — use `List<T>` (or, for primitives, a `PrimitiveArray` subtype like `ByteArray`, see
  below).
- Convention (not enforced): don't prefix field method names with `get`.

## `@ModelRoot`

`com.flipkart.krystal.model.ModelRoot`, `@Target(TYPE)`, applies to both interfaces and enums:

| Attribute | Default | Description |
|---|---|---|
| `type` | `{}` | `ModelType[]` — `REQUEST`, `RESPONSE`, or both. Empty = general-purpose, no request/response rules, nestable anywhere. |
| `pure` | `true` | Restricts field types to a closed, serde-friendly set. See Purity below. |
| `builderExtendsModelRoot` | `false` | If true, the generated `Builder` also extends the model root interface itself. See below. |
| `suffixSeparator` | `"_"` | Separator before generated suffixes (`_Immut`, `_ImmutPojo`, ...). |
| `isShared` | `false` | Marks the model as intended for cross-module/cross-project use (client modules may generate code in a subpackage to avoid split-package issues). Rarely set directly. |

### `type` and nested-model consistency

`REQUEST`-only models may only nest other `REQUEST`-or-dual-type models; `RESPONSE`-only models may only nest
`RESPONSE`-or-dual-type models; general-purpose (`type = {}`) models have no restriction and can be nested
anywhere. Violating this is a compile error:

```java
// ✗ Compile error: RESPONSE model references REQUEST-only model
@ModelRoot(type = {RESPONSE})
public interface MyResponse extends Model {
  MyRequest nested(); // ERROR — MyRequest is type = {REQUEST}
}

// ✓ nested model's type includes RESPONSE, or is general-purpose
@ModelRoot(type = {RESPONSE})
public interface MyResponse extends Model {
  InnerResponse nested();   // OK — type includes RESPONSE
  CommonData commonData();  // OK — CommonData is general-purpose
}
```

### `builderExtendsModelRoot`

```java
// default: interface Builder extends ImmutableModel.Builder { ... }
// builderExtendsModelRoot = true:
interface Builder extends ImmutableModel.Builder, MyModelRoot { ... }
```

A `Builder` instance then satisfies the model root type itself, so it can be passed anywhere the plain
interface is expected without calling `._build()` first — useful for a model that's an intermediate, mutable
representation in a processing pipeline (e.g. a REST framework injecting a partially-built response builder
into a Vajram — see `InnerDataV2`/`SubMessage` in `references/examples.md`). **Not allowed on enum model
roots** — compile-time checked.

## Field annotations

### `@SerialId`

`com.flipkart.krystal.serial.SerialId`, `int value()`. Assigns a stable binary index for protocols like
Protobuf/Fory. **All-or-none**: present on every field of a model, or none at all (falls back to declaration
order). `@SerialId(0)` is disallowed on interface-model fields (reserved for an enum's `UNKNOWN` constant, see
below).

### `@IfAbsent`

`com.flipkart.krystal.model.IfAbsent`. Documents/enforces behavior when a field has no value:

| Value | Meaning | Required field type |
|---|---|---|
| `FAIL` | Mandatory — building without it throws `MandatoryFieldMissingException`. | any |
| `MAY_FAIL_CONDITIONALLY` | Conditionally mandatory; needs `conditionalFailureInfo` string. **Not allowed in RESPONSE-only models** (compile error) — allowed in REQUEST-only and dual-type models. | any |
| `WILL_NEVER_FAIL` | Optional; code handles absence gracefully. | `@Nullable T` or `Optional<T>` |
| `ASSUME_DEFAULT_VALUE` | Absent ⇒ type-specific zero-like default (`0`, `""`, empty collection, `false`, empty model, or the enum's `@DefaultValue` constant). Enables wire-size optimizations (defaults aren't transmitted). | any (lists/maps need no `@Nullable`) |

Implicit default when `@IfAbsent` is omitted: `FAIL` for `RESPONSE`-only and general-purpose (`type = {}`)
models; `MAY_FAIL_CONDITIONALLY` for `REQUEST`-only models. **Dual-type (`{REQUEST, RESPONSE}`) models have no
implicit default — every field needs an explicit `@IfAbsent`, and it must satisfy both rule sets at once**
(e.g. a `WILL_NEVER_FAIL` field must still be `@Nullable`/`Optional` because the model is also a REQUEST).

`@Nullable` (checkerframework's `org.checkerframework.checker.nullness.qual.Nullable` in every real sample —
not `javax`/`jakarta`) or `Optional<T>` mark the return type for `WILL_NEVER_FAIL` fields.

## `@SupportedModelProtocol` — doc/source drift

`com.flipkart.krystal.model.SupportedModelProtocol`:

```java
@Repeatable(SupportedModelProtocols.class)
@Target(TYPE)
public @interface SupportedModelProtocol {
  Class<? extends ModelProtocol> value(); // single value, not an array
}
```

**The framework's own `Krystal-models.md` shows `@SupportedModelProtocol({PlainJavaObject.class,
Protobuf3.class})` — this does not compile.** `value()` takes one `Class`, not an array; the annotation is
`@Repeatable` instead, so multiple protocols are declared as multiple annotations. Every real sample in the
framework (lattice REST/gRPC samples, vajram-sql samples) uses the repeated form:

```java
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Json.class)
@SupportedModelProtocol(Protobuf3.class)
```

- Omitted/absent → only `_Immut`/`_Immut.Builder` generated.
- `PlainJavaObject` (`com.flipkart.krystal.model.PlainJavaObject`) → `_ImmutPojo`, generated in the same
  compiler phase as the model itself (`MODELS` phase). Must be listed explicitly — it's not implied by anything.
- `Json` (`com.flipkart.krystal.vajram.json.Json`) → `_ImmutJson`, a Jackson-backed `SerializableJsonModel`.
- `Protobuf3` (`com.flipkart.krystal.vajram.protobuf3.Protobuf3`) → `_ImmutProto`, wraps a generated protobuf
  message. Requires purity (`pure = true`) — compile error otherwise.
- `Fory` (`com.flipkart.krystal.vajram.fory.Fory`) → `_ImmutFory`, backed by Apache Fory (JIT-compiled
  object-graph serialization, no IDL). **Not mentioned in `Krystal-models.md`'s protocol table** — it's a real,
  fourth supported protocol; treat the doc's table as incomplete, not authoritative, on this point too.
- A nested model field must support **at least** the same protocol set as its parent (compile-time checked by
  `SerdeModelValidator`).
- `Json` and `Fory` implement `SerdeProtocol` (extends `ModelProtocol`); `PlainJavaObject` implements
  `ModelProtocol` directly (no actual serialization).

## Model purity

`@ModelRoot(pure = true)` is the default. A pure model's fields are restricted to:
- primitives/boxed primitives (`int`, `Integer`, `boolean`, ...)
- `String`
- `PrimitiveArray` subtypes (`ByteArray`, `IntArray`, `LongArray`, `DoubleArray` — `com.flipkart.krystal.model.array`;
  use these instead of raw arrays, since raw arrays are mutable and disallowed as field types entirely)
- other pure `@ModelRoot` models, and `@ModelRoot` enums (a plain Java enum without `@ModelRoot` is **not**
  allowed as a field type in a pure model)
- `List<T>`/`Map<K, V>` of the above

`pure = false` lifts these checks — needed for a model referencing a type outside the Krystal model system (a
third-party DTO). Auto-generated `REQUEST` models default to `pure = false`. Protocols whose
`ModelProtocol#modelsNeedToBePure()` returns `true` (Protobuf3, Fory) reject a non-pure model that declares
support for them — compile error, not a runtime surprise.

## Map field constraints

- Pure models: keys must be primitive/boxed/`String`/`@ModelRoot` enum.
- Any model supporting a `SerdeProtocol` (`Json`/`Protobuf3`/`Fory`): enum keys are **disallowed**, regardless
  of purity — an unknown key value on the wire can't be deserialized back to a specific enum constant reliably.
  So `Map<Priority, String>` compiles in a POJO-only model but fails once `Json`/`Protobuf3` is added.
- Protobuf3 additionally restricts keys to integral types and `String` (a proto3 limitation).

## Nested collections — always disallowed

`List<List<...>>`, `List<Map<...>>`, `Map<K, Map<...>>`, `Map<K, List<...>>` are compile errors regardless of
purity or protocol. Wrap the inner collection in its own `@ModelRoot` model:

```java
// ✗
List<List<String>> nestedList();

// ✓
@ModelRoot
public interface StringList extends Model {
  List<String> values();
}
// ...
List<StringList> nestedList();
```

## Enum model roots

```java
@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Protobuf3.class)
public enum Priority implements EnumModel {
  @SerialId(0) UNKNOWN,
  @SerialId(1) LOW,
  @SerialId(2) MEDIUM,
  @SerialId(3) HIGH
}
```

- `implements EnumModel` (`com.flipkart.krystal.model.EnumModel`) — a marker `extends ImmutableModel` with a
  no-op `Builder` (enums are already immutable).
- **`UNKNOWN` must be the first constant**, always.
- `@SerialId` all-or-none, same rule as interface models; if used, `UNKNOWN` must be `@SerialId(0)` and no
  duplicates. If omitted, binary index defaults to ordinal (declaration order), `UNKNOWN = 0`.
- Instead of (or in addition to, in real samples) `@SerialId(0)`, the framework's real samples mark the default
  constant with `@DefaultValue` (`com.flipkart.krystal.model.DefaultValue`, `@Target(FIELD)`, no attributes) —
  place it on the first (`UNKNOWN`) constant. This is what `IfAbsent.ASSUME_DEFAULT_VALUE` resolves to for an
  enum field.
- Pure models may only reference `@ModelRoot`-annotated enums as field types — a plain Java enum is rejected.
- JSON deserialization of an unrecognized string falls back to `UNKNOWN` instead of throwing (via
  `EnumModelModule` on the shared `ObjectMapper`) — this is what makes enum evolution backward-compatible for
  JSON consumers. Protobuf's `UNRECOGNIZED` falls back the same way, via a generated
  `<EnumName>_ProtoUtils.protoToJava`/`javaToProto` pair that every `_ImmutProto` wrapper referencing the enum
  delegates to.

## Generated artifacts

| Artifact | Phase | Condition |
|---|---|---|
| `<Model>_Immut` (interface) + nested `Builder` | MODELS | Always |
| `<Model>_ImmutPojo` (class + inner `Builder`) | MODELS | `PlainJavaObject` in `@SupportedModelProtocol` |
| `<Model>_ImmutJson` | FINAL | `Json` in `@SupportedModelProtocol` |
| `<Model>_ImmutProto` + `<Model>_Proto.proto` | FINAL | `Protobuf3` in `@SupportedModelProtocol` |
| `<Model>_ImmutFory` | FINAL | `Fory` in `@SupportedModelProtocol` |
| `<EnumName>_ProtoUtils` | FINAL | enum model root with `Protobuf3` support |

`_Immut.Builder` declares a setter per field and `_build()` returning `_Immut`. For a nested-model field
(non-enum), the builder additionally generates a getter (so a partially-built `List`/`Map` of nested models can
be read back) and an overloaded setter accepting the nested model's own `Builder` type directly — you don't
need to call `._build()` on a nested value before passing it in. Every serde-specific `Builder`
(`_ImmutPojo.Builder`, `_ImmutJson.Builder`, ...) implements the same `_Immut.Builder` contract, so code that
only touches the shared setters is protocol-agnostic.

## Other annotations you may see on a `ModelRoot`

- `@DefaultSerdeProtocol(Json.class)` (`com.flipkart.krystal.serial.DefaultSerdeProtocol`) — declares which
  serde protocol to use when a client doesn't specify one explicitly (e.g. no `Accept` header). Purely an
  application-layer defaulting hint; changing it is a backward-incompatible change (clients that were getting
  one wire format suddenly get another) — treat it with the same caution as changing a field's optionality.
- `@ModelClusterRoot(immutableRoot = ..., builderRoot = ...)` — an advanced escape hatch for a model hierarchy
  whose immutable/`Builder` root types aren't the codegen-generated ones (used by framework extensions like
  vajram-graphql for their own hand-written `_Immut`/`Builder` base types). You will not need this for an
  ordinary application-level model.
- `@InputsForVajram`/`VajramInputs<T>` (`com.flipkart.krystal.facets`) — lets a Vajram's `_Inputs` be declared in
  a standalone interface rather than nested inside the Vajram class (for sharing one input contract across
  multiple Vajrams, or splitting it into a different module). This is a Vajram-authoring concern layered on top
  of modelling, not a `@ModelRoot` feature itself — see the vajram-authoring skill.

## Build wiring

The MODELS-phase generator lives in the same `vajram-codegen` Gradle/Maven module used for Vajrams — if the
module already builds Vajrams, `@ModelRoot`/`_Immut`/`_ImmutPojo` generation already works. Each additional
serde protocol needs its own codegen artifact on the annotation-processor classpath (Gradle's
`krystalModelsGenProcessor` configuration, with the `com.flipkart.krystal` Gradle plugin applied):

```kotlin
dependencies {
    implementation("com.flipkart.krystal:vajram-java-sdk")
    implementation("com.flipkart.krystal:vajram-json")       // if using Json
    implementation("com.flipkart.krystal:vajram-protobuf3")  // if using Protobuf3

    krystalModelsGenProcessor("com.flipkart.krystal:vajram-codegen")        // always
    krystalModelsGenProcessor("com.flipkart.krystal:vajram-json-codegen")       // Json
    krystalModelsGenProcessor("com.flipkart.krystal:vajram-protobuf3-codegen")  // Protobuf3
    krystalModelsGenProcessor("com.flipkart.krystal:vajram-fory-codegen")       // Fory
}
```

Match whichever dependency-declaration style (version-catalog aliases, a BOM/platform, or raw coordinates) this
repo already uses for its other Krystal dependencies rather than introducing a new form. A bare `@ModelRoot`
with no `@SupportedModelProtocol` produces no visible output in `build/generated` beyond `_Immut` — that's
expected, not a sign the wiring is broken; it only becomes visible once a protocol is added or the model is
actually referenced from a Vajram/REST endpoint.
