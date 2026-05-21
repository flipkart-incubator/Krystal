# vajram-fory

**Apache Fory SerdeProtocol for Krystal Vajram SDK**

## Overview

This module provides the runtime classes that enable [Apache Fory](https://fory.apache.org/)
as a serialization protocol within the Krystal modelling framework.

Apache Fory is a blazing-fast, JIT-compiled, cross-language serialization framework.
Unlike schema-based protocols (protobuf), Fory serializes plain Java object graphs
directly — no IDL files or external code generators are required.  The JIT compiler
produces optimised (de)serializers at runtime, making Fory ideal for same-language,
high-throughput pipelines.

## Design Philosophy

| Aspect              | Fory                                  | JSON (Jackson)           | Protobuf               |
|---------------------|---------------------------------------|--------------------------|------------------------|
| Schema / IDL        | **None** — works with Java POJOs      | None (annotations)       | `.proto` files          |
| Code generation     | `_ImmutFory` wrapper (JavaPoet)       | `_ImmutJson` wrapper     | `.proto` → protoc → wrapper |
| Wire format         | Fory binary (JIT-optimised)           | JSON text                | Protobuf binary         |
| Codegen phase       | `MODELS`                              | `MODELS`                 | `MODELS` + `FINAL`      |
| Purity required     | No                                    | No                       | Yes                     |

Because Fory serializes Java objects natively, there is no separate schema-generation
step.  The `_ImmutFory` class **is** both the Krystal model implementation and the
serialization target.

## Key Classes

- **`Fory`** — `SerdeProtocol` singleton.  Exposes the shared, thread-safe Fory
  instance via `Fory.foryInstance()`.
- **`SerializableForyModel`** — marker interface that generated `_ImmutFory` classes
  implement; provides the default `_serdeProtocol()` returning `Fory.FORY`.

## Usage

```java
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol({Fory.class, PlainJavaObject.class})
public interface MyResponse extends Model {
    String greeting();
    int count();
}
```

The Krystal codegen (with `vajram-fory-codegen` on the annotation-processor path) will
generate `MyResponse_ImmutFory` which round-trips through Fory binary:

```java
MyResponse_ImmutFory resp = MyResponse_ImmutFory._builder()
    .greeting("Hello")
    .count(42)
    ._build();

byte[] bytes = resp._serialize();
MyResponse_ImmutFory restored = new MyResponse_ImmutFory(bytes);
assert restored.greeting().equals("Hello");
```

## Module Coordinates

```
com.flipkart.krystal:vajram-fory
```
