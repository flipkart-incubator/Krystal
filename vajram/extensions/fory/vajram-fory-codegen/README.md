# vajram-fory-codegen

**Annotation-processor codegen for the Apache Fory SerdeProtocol extension**

## Overview

This module plugs into the Krystal codegen pipeline via the
`ModelsCodeGeneratorProvider` SPI.  When the annotation processor detects a
`@ModelRoot` annotated with `@SupportedModelProtocols(Fory.class)`, this
generator produces an `_ImmutFory` wrapper class for that model.

## Architecture

```
@ModelRoot + @SupportedModelProtocols(Fory.class)
        │
        ▼
  ForyModelGenProvider  (SPI — discovered via AutoService)
        │
        ▼
  ForyModelsGen.generate()
        │
        ▼
  Foo_ImmutFory.java  (generated source file)
```

### Codegen Phase

All Fory code generation happens in the **MODELS** phase.  Because Fory
serializes plain Java POJOs directly, there is no separate schema-generation
step (contrast with protobuf which generates `.proto` files in MODELS and
wrapper classes in FINAL).

### Generated Class Layout

For a model `Foo`:

```
Foo_ImmutFory
├── static final ThreadSafeFory _FORY      // shared Fory instance
├── transient byte[] _serializedPayload    // cached serialized form
├── transient boolean _deserializationPending
│
├── Foo_ImmutFory(byte[])                  // construct from bytes (lazy)
├── Foo_ImmutFory(field1, field2, …)       // all-args constructor
├── Foo_ImmutFory(Foo _from)               // copy constructor
│
├── _serialize() → byte[]                  // Fory.serialize(this)
├── _deserialize()                         // lazy: Fory.deserialize → copy fields
│
├── field getters                          // call _deserialize() first
├── _asBuilder(), _build(), _newCopy()
│
└── static class Builder
    ├── fields
    ├── getters / setters (fluent)
    └── _build() → Foo_ImmutFory
```

### Key Design Decisions

1. **`transient` meta-fields** — `_serializedPayload` and
   `_deserializationPending` are `transient`, so Fory ignores them during
   serialization.  Only the model's data fields are on the wire.

2. **Lazy deserialization via copy** — Unlike Jackson which can mutate an
   existing object in place, Fory always creates a new object on
   deserialization.  The generated `_deserialize()` method deserializes into
   a temporary instance and copies each field.

3. **Nested models** — Nested `Model` fields are converted to their
   `_ImmutFory` counterpart in setters, ensuring the full object graph is
   Fory-serializable.

4. **Enum models** — Skipped by this generator.  Fory handles Java enums
   natively — no wrapper is needed.

## Module Coordinates

```
com.flipkart.krystal:vajram-fory-codegen
```

Add as an annotation-processor dependency:

```groovy
krystalModelsGenProcessor 'com.flipkart.krystal:vajram-fory-codegen'
```
