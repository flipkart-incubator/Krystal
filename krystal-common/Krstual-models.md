# Krystal Models

## Enums

Krystal supports enum types as part of its modelling framework. An enum model is a Java `enum` annotated with `@ModelRoot` that implements the `EnumModel` marker interface.

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

- **UNKNOWN must be the first constant.** Every `@ModelRoot` enum must declare `UNKNOWN` as its first value.
- **`@SerialId` rules.** If `@SerialId` is used on any enum constant, `UNKNOWN` must have `@SerialId(0)`. Duplicate `@SerialId` values are not allowed. If `@SerialId` is not used, the proto index defaults to the ordinal (declaration order), with `UNKNOWN = 0`.
- **Pure models.** Pure models (where `@ModelRoot(pure = true)`) can only reference enums that themselves carry a `@ModelRoot` annotation. Plain Java enums without `@ModelRoot` are not allowed as field types in pure models.

### JSON Serialization

- **Serialization:** Jackson's `ObjectMapper` serializes enum values by name as usual.
- **Deserialization:** If the JSON contains a string that does not match any enum constant, it is deserialized to `UNKNOWN` instead of throwing an error. This is handled by the `EnumModelModule` registered on the shared `ObjectMapper`.

### Protobuf Serialization

- **Schema generation:** `Proto3SchemaGen` generates a `.proto` enum definition for each `@ModelRoot` enum. The proto enum name is the Java enum name with a `_Proto` suffix (same convention as messages).
- **Index assignment:** Proto indices follow declaration order (ordinal) unless overridden by `@SerialId` annotations.
- **Proto to Java mediation:** The `ProtoEnumUtils` utility provides name-based conversion between proto-generated enum values and Java enum values. Unknown proto values (e.g., `UNRECOGNIZED`) fall back to `UNKNOWN`.

### Usage as a Field in Models

Enum models can be used as fields in other `@ModelRoot` models:

```java
@ModelRoot
@SupportedModelProtocols({PlainJavaObject.class, Json.class, Protobuf3.class})
public interface Task extends Model {
  @SerialId(1) String name();
  @SerialId(2) Priority priority();
}
```

When used in protobuf messages, the enum field is generated as the corresponding proto enum type, and getter/setter code in the generated `_ImmutProto` wrapper handles conversion automatically.