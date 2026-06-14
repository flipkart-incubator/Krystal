# rest-fory-quarkus-lattice-sample

**Sample Lattice application demonstrating Apache Fory serde integration**

## Overview

This sample application shows how to use [Apache Fory](https://fory.apache.org/)
as a serialization protocol within a Krystal Lattice application running on
Quarkus.  It mirrors the structure of the JSON sample
(`rest-json-quarkus-lattice-sample`) but uses Fory binary format for request
and response serialization.

## What This Demonstrates

1. **Model declarations** — `@SupportedModelProtocol({Fory.class, PlainJavaObject.class})`
   on `@ModelRoot` interfaces triggers generation of `_ImmutFory` wrapper classes.
2. **GET endpoint** (`ForyGetSample`) — accepts path/query params and returns
   a `ForyResponse` serialized via Fory binary.
3. **POST endpoint** (`ForyPostSample`) — accepts a Fory-serialized
   `ForyRequest` body (`Content-Type: application/x-fory`) and echoes the fields.
4. **Unit tests** (`ForyResponseTest`) — exercise `_ImmutFory` builder →
   `_serialize()` → constructor-from-bytes round-trip with various field types:
   primitives, optionals, lists, maps, nested models.
5. **Integration tests** (`ForyQuarkusE2eTest`) — boot the Quarkus server,
   send HTTP requests with Fory binary payloads, and verify responses.

## Running

```bash
# Publish Krystal to local Maven repo first (from project root)
./upgradeVersionLocal.macOS.sh

# Run the sample tests
./gradlew :lattice:samples:rest:fory:quarkus:rest-fory-quarkus-lattice-sample:test -PunsafeCompile=true
```

## Module Coordinates

```
com.flipkart.krystal:rest-fory-quarkus-lattice-sample
```

(This is a sample module — it is not published to Maven Central.)
