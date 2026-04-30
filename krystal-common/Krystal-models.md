# Krystal Models

## Interface ModelRoots

An interface model root is a Java `interface` annotated with `@ModelRoot` that extends `Model`. It
defines a structured data model whose fields are declared as zero-argument accessor methods. The
Krystal code generation framework processes these interfaces at compile time and generates immutable
implementations, builders, and serde wrappers.

### Defining a Model Root

```java

@ModelRoot(type = RESPONSE)
@SupportedModelProtocols({PlainJavaObject.class, Protobuf3.class})
public interface OrderResponse extends Model {

  @SerialId(1)
  @IfAbsent(FAIL)
  String orderId();

  @SerialId(2)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable
  String trackingUrl();

  @SerialId(3)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<LineItem> items();

  @SerialId(4)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, String> metadata();
}
```

**Rules for model root interfaces:**

- Must extend `Model`.
- All accessor methods must have zero parameters and a non-void return type.
- Array return types are not allowed — use `List<T>` instead (byte arrays use `PrimitiveArray`
  subtypes like `ByteArray`).
- Method names should _not_ use the `get` prefix (recommended convention, not enforced).

### `@ModelRoot` Annotation

| Attribute                 | Default      | Description                                                                                                                                                                                                                                                                                                                                          |
|---------------------------|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `type`                    | `{}` (empty) | An array of `ModelType` values: `REQUEST`, `RESPONSE`, or both `{REQUEST, RESPONSE}`. An empty array (the default) means the model is a general-purpose model with no request/response-specific semantics. `REQUEST` models are used to accept data from a client. `RESPONSE` models are used to provide data to a client. Models with both types must adhere to **both** sets of constraints. |
| `pure`                    | `true`       | If true, all field types are restricted to primitives, boxed primitives, `String`, `PrimitiveArray` subtypes, `@ModelRoot` enums, pure `Model`s, and `List`/`Map` of these types. See [Model Purity](#model-purity).                                                                                                                                 |
| `builderExtendsModelRoot` | `false`      | If true, the generated `Builder` interface also extends the model root interface, so builder instances can be used wherever the model root type is expected. See [Builder Extends ModelRoot](#builderExtendsModelRoot).                                                                                                                               |
| `suffixSeparator`         | `"_"`        | The separator between the model root name and the generated class suffix (e.g. `_Immut`, `_ImmutPojo`).                                                                                                                                                                                                                                              |

### Field Annotations

#### `@SerialId`

Assigns a stable unique numeric identifier to a field for binary serde protocols (e.g. Protobuf). If
used on any field, it must be used on _all_ fields (all-or-none rule). If omitted entirely, serde
protocols fall back to declaration order. `@SerialId(0)` is not allowed in interface models (it is
allowed in enum models to denote default enum value; See [Enum ModelRoots](#enum-modelroots))

#### `@IfAbsent`

Documents the behavior when a field has no value. The possible strategies are:

- **`FAIL`** — The field is mandatory. Building the model without this field throws
  `MandatoryFieldMissingException`. In `REQUEST` models, this implies that the API accepting the
  request will fail if the value is null. In `RESPONSE` models, it means the API will always return
  a non-null value. In `RESPONSE` models and general-purpose models (empty `type`), this is the
  default value for fields which don't have `@IfAbsent` annotation.
- **`MAY_FAIL_CONDITIONALLY`** — The field is conditionally mandatory. The API may fail in certain
  conditions if the value is null. The documentation of the field should specify this behaviour. In
  `REQUEST`-only models, this is the default value for fields which don't have `@IfAbsent`
  annotation. **This value is not allowed in `RESPONSE`-only models** — a compile error is emitted
  if used. However, it is allowed in dual-type `{REQUEST, RESPONSE}` models since they also serve
  as REQUEST models.
- **`WILL_NEVER_FAIL`** — The field is optional. The code handles the missing case gracefully. The
  field type should be `Optional<T>` or `@Nullable T`. In `REQUEST` models it means the API will
  never fail if this field is `null`. In `RESPONSE` models it means that the API may return `null`
  for this field.
- **`ASSUME_DEFAULT_VALUE`** — If absent, a type-specific default is assumed (0 for numbers, empty
  string, empty list/map, false for booleans, empty model for models). This enables serialization
  optimizations in protocols like Protobuf where defaults are not transmitted on the wire.

**Models with `type = {REQUEST, RESPONSE}`:** Since `REQUEST` and `RESPONSE` have different
`@IfAbsent` defaults, models with both types **must** have an explicit `@IfAbsent` annotation on
every field. Omitting it triggers a compile error. Such models must also satisfy both sets of
constraints — for example, `WILL_NEVER_FAIL` fields must still be `Optional` or `@Nullable`
because the model includes `REQUEST`. `MAY_FAIL_CONDITIONALLY` is allowed since the model also
includes `REQUEST`.

#### `@Nullable` and `Optional<T>`

Fields that can be absent should declare their return type as `@Nullable T` or `Optional<T>`. Lists
and maps with `@IfAbsent(ASSUME_DEFAULT_VALUE)` default to empty collections and do not need
`@Nullable`.

### Generated Code — The `MODELS` Phase

The Krystal annotation processor runs in two phases: **`MODELS`** and **`FINAL`**.

During the **`MODELS`** phase, `JavaModelsGen` generates the following for each interface model
root:

#### 1. `<ModelRoot>_Immut` — The Immutable Interface

An interface that extends both the model root and `ImmutableModel`. Its `_build()` method returns
`this` (since an immutable model is already built).

```
OrderResponse_Immut extends OrderResponse, ImmutableModel
```

This interface also contains a nested `Builder` interface.

#### 2. `<ModelRoot>_Immut.Builder` — The Builder Interface

A nested interface inside `_Immut` that extends `ImmutableModel.Builder`. It declares a setter for
each field and a `_build()` method that returns the `_Immut` type.

```java
interface Builder extends ImmutableModel.Builder {

  Builder orderId(@Nullable String orderId);

  Builder trackingUrl(@Nullable String trackingUrl);

  Builder items(@Nullable List<LineItem> items);

  Builder metadata(@Nullable Map<String, String> metadata);

  OrderResponse_Immut _build();

  Builder _newCopy();
}
```

For fields whose value type is a nested model root (not an enum), the builder also generates:

- A **getter** for container fields (`List` / `Map`), so partially-built collections can be read
  back.
- An **overloaded setter** that accepts the nested model's builder type directly (e.g.
  `Builder lineItem(LineItem_Immut.Builder lineItem)`).

#### 3. `<ModelRoot>_ImmutPojo` — The POJO Implementation

Generated only if `PlainJavaObject` is explicitly listed in `@SupportedModelProtocols`.
This is a `final` class that implements `_Immut` with one field
per accessor. It includes:

- A static `_builder()` factory method returning its inner `Builder`.
- `_asBuilder()` to convert the immutable instance back to a mutable builder.
- `_newCopy()` for deep-copying.
- `equals()`, `hashCode()`, and `toString()` implementations.

Its inner `Builder` class implements `_Immut.Builder`, validates `@IfAbsent` constraints on
`_build()`, and throws `MandatoryFieldMissingException` for missing mandatory fields.

### Serde Implementations — The `FINAL` Phase

During the **`FINAL`** phase, serde-specific code generators produce additional implementations
based on the model's `@SupportedModelProtocols`:

| Protocol          | Generated class | Description                                                                                                                                                                                                                                                                          |
|-------------------|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PlainJavaObject` | `_ImmutPojo`    | In-memory POJO (generated in MODELS phase, see above).                                                                                                                                                                                                                               |
| `Json`            | `_ImmutJson`    | Jackson-annotated wrapper. Implements `SerializableJsonModel`. Uses `@JsonProperty` and `@JsonDeserialize` for nested models. Deserialization of unknown enum values falls back to `UNKNOWN`.                                                                                        |
| `Protobuf3`       | `_ImmutProto`   | Wraps a generated protobuf `_Proto` message builder. Implements `SerializableModel`. Getters and setters translate between proto types and Java types. For nested models, wraps/unwraps `_ImmutProto` instances. For enum fields, uses generated `<Enum>_ProtoUtils` for conversion. |

Each serde implementation provides its own `Builder` inner class that also implements
`_Immut.Builder`, so all implementations share the same builder contract.

### `@SupportedModelProtocols`

Lists which `ModelProtocol` implementations the model supports:

```java
@SupportedModelProtocols({PlainJavaObject.class, Json.class, Protobuf3.class})
```

- If absent or empty, only the `_Immut` interface is generated (no POJO, no serde implementations).
  `PlainJavaObject` must be explicitly listed to get POJO generation.
- Each protocol listed must have a corresponding code generator on the annotation processor
  classpath.
- Nested model fields must support at least the same serde protocols as the parent model — this is
  validated at compile time by `SerdeModelValidator`.

**Protocol types:**

- **`ModelProtocol`** — base interface. `PlainJavaObject` implements this directly (no
  serialization).
- **`SerdeProtocol`** — extends `ModelProtocol`. `Json` and `Protobuf3` implement this. Models that
  support a `SerdeProtocol` are subject to additional constraints (e.g. enum map keys are
  disallowed — see below).

### Model Purity

By default, `@ModelRoot(pure = true)`. A pure model restricts field types to a closed set that all
serde protocols can reliably handle:

- Primitives and boxed primitives (`int`, `Integer`, `boolean`, `Boolean`, etc.)
- `String`
- `PrimitiveArray` subtypes (e.g. `ByteArray`)
- `@ModelRoot` enums (i.e. enums implementing `EnumModel`)
- Other pure `@ModelRoot` models
- `List<T>` or `Map<K, V>` of the above types

Setting `pure = false` disables these checks. This is needed for models that reference types outside
the Krystal model system (e.g. third-party DTOs). Auto-generated `REQUEST` models are created with
`pure = false`.

Some serde protocols (like `Protobuf3`) require purity — a compile error is emitted if a non-pure
model declares support for such a protocol.

### `builderExtendsModelRoot`

When `@ModelRoot(builderExtendsModelRoot = true)`, the generated `Builder` interface extends the
model root interface _in addition to_ `ImmutableModel.Builder`:

```java
// Default (builderExtendsModelRoot = false):
interface Builder extends ImmutableModel.Builder { ...
}

// With builderExtendsModelRoot = true:
interface Builder extends ImmutableModel.Builder, SubMessage { ...
}
```

This means a `Builder` instance satisfies the model root type, so it can be passed to APIs that
accept the model root without calling `_build()` first. This is useful for models that serve as
intermediate, mutable representations in a processing pipeline.

**Constraints:**

- Enum model roots must **not** use `builderExtendsModelRoot = true` — this is validated at compile
  time.

### Nested Model Type Consistency

`REQUEST` models can only contain nested models whose `type` includes `REQUEST`. Similarly,
`RESPONSE` models can only contain nested models whose `type` includes `RESPONSE`. Models with
empty `type` (general-purpose) can be nested anywhere and have no restrictions on their nested
models. This is validated at compile time.

For example, a `RESPONSE` model cannot have a field whose type is a `REQUEST`-only model:

```java
// ✗ Compile error: RESPONSE model references REQUEST-only model
@ModelRoot(type = {RESPONSE})
public interface MyResponse extends Model {
  MyRequest nested(); // ERROR — MyRequest is type = {REQUEST}
}

// ✓ Correct: nested model includes RESPONSE
@ModelRoot(type = {RESPONSE})
public interface MyResponse extends Model {
  InnerResponse nested(); // OK — InnerResponse type includes RESPONSE
}

// ✓ Correct: nested model has both types
@ModelRoot(type = {RESPONSE})
public interface MyResponse extends Model {
  SharedData nested(); // OK — SharedData type = {REQUEST, RESPONSE}
}

// ✓ Correct: general-purpose models can be nested anywhere
@ModelRoot(type = {REQUEST})
public interface MyRequest extends Model {
  CommonData nested(); // OK — CommonData type = {} (general-purpose)
}
```

### Map Field Constraints

Map fields have the following key-type restrictions:

- **Pure models:** Map keys must be primitives, boxed primitives, `String`, or `@ModelRoot` enums.
- **Models with serde protocols:** `EnumModel` map keys are **not allowed**, because unknown enum
  key values cannot be deserialized reliably. This is enforced at compile time. For example,
  `Map<Priority, String>` is allowed in a POJO-only model but rejected if the model supports `Json`
  or `Protobuf3`.
- **Protobuf3 specifically:** Only integral types and `String` are valid map keys (a proto3
  limitation).

### Summary of Generated Artifacts

For a model root `MyModel`:

| Artifact                                     | Phase  | Condition                                                      |
|----------------------------------------------|--------|----------------------------------------------------------------|
| `MyModel_Immut` (interface)                  | MODELS | Always                                                         |
| `MyModel_Immut.Builder` (interface)          | MODELS | Always                                                         |
| `MyModel_ImmutPojo` (class + inner Builder)  | MODELS | `PlainJavaObject` explicitly in `@SupportedModelProtocols`     |
| `MyModel_ImmutJson` (class + inner Builder)  | FINAL  | `Json` in `@SupportedModelProtocols`                           |
| `MyModel_ImmutProto` (class + inner Builder) | FINAL  | `Protobuf3` in `@SupportedModelProtocols`                      |
| `MyModel_Proto.proto` (schema)               | FINAL  | `Protobuf3` in `@SupportedModelProtocols`                      |

## Enum ModelRoots

Krystal supports enum types as part of its modelling framework. An enum model is a Java `enum`
annotated with `@ModelRoot` that implements the `EnumModel` marker interface.

### Definition

```java

@ModelRoot
@SupportedModelProtocols({PlainJavaObject.class, Json.class, Protobuf3.class})
public enum Priority implements EnumModel {
  @SerialId(0) UNKNOWN,
  @SerialId(1) LOW,
  @SerialId(2) MEDIUM,
  @SerialId(3) HIGH
}
```

### Constraints

- **UNKNOWN must be the first constant.** Every `@ModelRoot` enum must declare `UNKNOWN` as its
  first value.
- **`@SerialId` rules.** `@SerialId` must be present on all constants or none — partial usage is not
  allowed. If `@SerialId` is used, `UNKNOWN` must have `@SerialId(0)`, and duplicate values are not
  allowed. If `@SerialId` is not used, the binary index used by serde protocols defaults to the
  ordinal (declaration order), with `UNKNOWN = 0`. This all-or-none rule applies to all `@ModelRoot`
  types (both enums and interface models).
- **Pure models.** Pure models (where `@ModelRoot(pure = true)`) can only reference enums that
  themselves carry a `@ModelRoot` annotation. Plain Java enums without `@ModelRoot` are not allowed
  as field types in pure models.

### JSON Serialization

- **Serialization:** Jackson's `ObjectMapper` serializes enum values by name as usual.
- **Deserialization:** If the JSON contains a string that does not match any enum constant, it is
  deserialized to `UNKNOWN` instead of throwing an error. This is handled by the `EnumModelModule`
  registered on the shared `ObjectMapper`.

### Protobuf Serialization

- **Schema generation:** `Proto3SchemaGen` generates a `.proto` enum definition for each
  `@ModelRoot` enum. The proto enum name is the Java enum name with a `_Proto` suffix (same
  convention as messages).
- **Index assignment:** Proto indices follow declaration order (ordinal) unless overridden by
  `@SerialId` annotations.
- **Proto to Java mediation:** For each `@ModelRoot` enum that supports Protobuf3, a
  `<EnumName>_ProtoUtils` utility class is generated with `protoToJava` and `javaToProto` static
  methods. These use switch expressions with enum constants for performant conversion. Unknown proto
  values (e.g., `UNRECOGNIZED`) fall back to `UNKNOWN`. All generated `_ImmutProto` wrappers that
  reference the enum delegate to these shared utility methods.

### Usage as a Field in Models

Enum models can be used as fields in other `@ModelRoot` models:

```java

@ModelRoot
@SupportedModelProtocols({PlainJavaObject.class, Json.class, Protobuf3.class})
public interface Task extends Model {

  @SerialId(1)
  String name();

  @SerialId(2)
  Priority priority();
}
```

When used in protobuf messages, the enum field is generated as the corresponding proto enum type,
and getter/setter code in the generated `_ImmutProto` wrapper delegates to the shared
`<EnumName>_ProtoUtils` class for conversion.