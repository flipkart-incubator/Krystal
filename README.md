# The Krystal Project

## Latest Releases

### Krystal BOM

[![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/krystal-bom.svg?label=krystal-bom&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/krystal-bom)

* Software Bill-Of-Materials artefact with all krystal artefacts inclusing extensions
* Excludes lattice related artefacts
* Use if you want Vajram, krystex and/or krystal models:  

### Lattice BOM

[![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-bom.svg?label=lattice-bom&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-bom)

* Software Bill-Of-Materials artefact with all krystal & Lattice artefacts, including extensions
* Use if you want lattice application framework along with vajram, krystex and krystal models: 

## Sub Modules

The monorepo is organized into publishable library modules (sample projects under `*-samples` are
omitted). Each entry below lists what the module contains, when to use it, its Maven coordinates,
project location, and BOM membership. Green coordinate badges denote [Krystal BOM](#krystal-bom)
artefacts; blue badges denote [Lattice BOM](#lattice-bom) artefacts; uncoloured badges are published
separately and not yet managed by a BOM.

### Core

#### **krystal-common**
* **What it has**
  * Common annotations and classes used across all Krystal modules
  * Basic interfaces and spec for the [Krystal modelling framework](krystal-common/Krystal-models.md)
* **When to use**
  * To model in-memory data with the Krystal modelling framework
  * Automatically pulled in as an API dependency when using any other Krystal module
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/krystal-common.svg?label=krystal-common&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/krystal-common)
* **Location:** [./krystal-common](krystal-common)
* **BOM:** [Krystal BOM](#krystal-bom)

#### **krystal-codegen-common**
* **What it has**
  * Shared utilities for Krystal annotation processors and code generators
* **When to use**
  * As a transitive dependency when using `vajram-codegen` or other Krystal codegen modules
  * Rarely added directly by application developers
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/krystal-codegen-common.svg?label=krystal-codegen-common&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/krystal-codegen-common)
* **Location:** [./krystal-codegen-common](krystal-codegen-common)
* **BOM:** [Krystal BOM](#krystal-bom)

#### **krystal-visualization**
* **What it has**
  * Tools to generate visual representations of Krystal graphs (for example, static HTML call graphs)
* **When to use**
  * To inspect or document the dependency structure of a vajram graph at build or dev time
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/krystal-visualization.svg?label=krystal-visualization&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/krystal-visualization)
* **Location:** [./krystal-visualization](krystal-visualization)
* **BOM:** [Krystal BOM](#krystal-bom)

#### **krystal-gradle-plugin**
* **What it has**
  * The `com.flipkart.krystal` Gradle plugin and Krystal-scoped build tasks
  * Wires `krystalModelsGen` into `compileJava` so models and vajram implementations are generated on every compile
* **When to use**
  * In every Gradle project that uses Vajram codegen — apply `id 'com.flipkart.krystal'` in `build.gradle`
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/com.flipkart.krystal.gradle.plugin.svg?label=com.flipkart.krystal.gradle.plugin&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/com.flipkart.krystal.gradle.plugin)
* **Location:** [./krystal-gradle-plugin](krystal-gradle-plugin)
* **BOM:** [Krystal BOM](#krystal-bom)

### Krystex

#### **krystex** (**Kryst**al **Ex**ecutor)
* **What it has**
  * The generic runtime execution engine for synchronous workflow DAGs (kryons, executors, decorators)
* **When to use**
  * To execute vajram-based business logic at runtime; keeps `vajram-java-sdk` and `krystex` independent of each other
  * When building a custom executor integration on top of Krystex primitives
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/krystex.svg?label=krystex&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/krystex)
* **Location:** [./krystex](krystex)
* **README:** [krystex/README.md](krystex/README.md)

### Vajram

#### **vajram-java-sdk**
* **What it has**
  * The Vajram programming model — annotations, base types, and APIs for defining synchronous scatter-gather business logic
* **When to use**
  * Whenever you write vajrams (the primary artefact for application business logic)
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-java-sdk.svg?label=vajram-java-sdk&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-java-sdk)
* **Location:** [./vajram/vajram-java-sdk](vajram/vajram-java-sdk)
* **BOM:** [Krystal BOM](#krystal-bom)
* **README:** [vajram-java-sdk/README.md](vajram/vajram-java-sdk/README.md)

#### **vajram-lang**
* **What it has**
  * An experimental standalone language embodying Krystal design philosophy (not the primary Java SDK)
* **When to use**
  * For experimentation with the vajram-lang programming model; production workflows should use `vajram-java-sdk`
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-lang.svg?label=vajram-lang&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-lang)
* **Location:** [./vajram/vajram-lang](vajram/vajram-lang)

#### **vajram-codegen-common**
* **What it has**
  * Shared codegen utilities specific to the Vajram annotation model
* **When to use**
  * As a transitive dependency of `vajram-codegen` and Vajram extension codegen modules
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-codegen-common.svg?label=vajram-codegen-common&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-codegen-common)
* **Location:** [./vajram/vajram-codegen-common](vajram/vajram-codegen-common)
* **BOM:** [Krystal BOM](#krystal-bom)

#### **vajram-codegen**
* **What it has**
  * Vajram annotation processors and validators that generate vajram models and implementation stubs at compile time
* **When to use**
  * If you are writing vajrams, add this as a `krystalModelsGenProcessor` dependency in every project that defines vajrams; pair with the `com.flipkart.krystal` Gradle plugin
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-codegen.svg?label=vajram-codegen&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-codegen)
* **Location:** [./vajram/vajram-codegen](vajram/vajram-codegen)
* **BOM:** [Krystal BOM](#krystal-bom)

### Vajram extensions

#### **vajram-guice**
* **What it has**
  * A kryon decorator that resolves `@Inject` facet values from a Guice injector
* **When to use**
  * When wiring vajram `@Inject` facets from a Guice-managed application context (see `vajram-samples`)
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-guice.svg?label=vajram-guice&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-guice)
* **Location:** [./vajram/extensions/guice/vajram-guice](vajram/extensions/guice/vajram-guice)
* **BOM:** [Krystal BOM](#krystal-bom)

#### **vajram-cdi**
* **What it has**
  * Jakarta CDI integration for injecting facet values into vajrams
* **When to use**
  * When your runtime uses CDI (for example, Quarkus or other Jakarta EE containers) instead of Guice
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-cdi.svg?label=vajram-cdi&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-cdi)
* **Location:** [./vajram/extensions/cdi/vajram-cdi](vajram/extensions/cdi/vajram-cdi)

#### **vajram-json** / **vajram-json-codegen**
* **What it has**
  * JSON (Jackson) as a `SerdeProtocol` for Krystal models, plus the annotation processor that generates `_ImmutJson` wrappers
* **When to use**
  * For human-readable wire formats, REST/JSON APIs, or debugging serialized model payloads
* **Coordinates:**
[![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-json.svg?label=vajram-json&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-json) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-json-codegen.svg?label=vajram-json-codegen&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-json-codegen)
* **Location:** [./vajram/extensions/json/vajram-json](vajram/extensions/json/vajram-json) · [./vajram/extensions/json/vajram-json-codegen](vajram/extensions/json/vajram-json-codegen)
* **BOM:** [Krystal BOM](#krystal-bom)

**vajram-fory** / **vajram-fory-codegen**
* **What it has**
  * [Apache Fory](https://fory.apache.org/) binary serialization as a `SerdeProtocol`, plus codegen for `_ImmutFory` wrappers
* **When to use**
  * For high-throughput, same-language pipelines that need fast binary serialization without `.proto` schemas
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-fory.svg?label=vajram-fory&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-fory) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-fory-codegen.svg?label=vajram-fory-codegen&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-fory-codegen)
* **Location:** [./vajram/extensions/fory/vajram-fory](vajram/extensions/fory/vajram-fory) · [./vajram/extensions/fory/vajram-fory-codegen](vajram/extensions/fory/vajram-fory-codegen)
* **BOM:** [Krystal BOM](#krystal-bom)
* **README:** [vajram-fory/README.md](vajram/extensions/fory/vajram-fory/README.md) · [vajram-fory-codegen/README.md](vajram/extensions/fory/vajram-fory-codegen/README.md)

**vajram-protobuf-util** / **vajram-protobuf-codegen-util**
* **What it has**
  * Protocol-agnostic runtime and codegen helpers shared by all Krystal Protobuf `SerdeProtocol` implementations
* **When to use**
  * Pulled transitively by protobuf3 or protobuf 2024 edition modules; rarely declared directly
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-protobuf-util.svg?label=vajram-protobuf-util&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-protobuf-util) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-protobuf-codegen-util.svg?label=vajram-protobuf-codegen-util&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-protobuf-codegen-util)
* **Location:** [./vajram/extensions/protobuf/vajram-protobuf-util](vajram/extensions/protobuf/vajram-protobuf-util) · [./vajram/extensions/protobuf/vajram-protobuf-codegen-util](vajram/extensions/protobuf/vajram-protobuf-codegen-util)
* **BOM:** [Krystal BOM](#krystal-bom)

**vajram-protobuf3** / **vajram-protobuf3-codegen**
* **What it has**
  * Proto3 `SerdeProtocol` support and codegen that generates `.proto` schemas and protobuf-backed model wrappers
* **When to use**
  * For schema-evolvable binary contracts, gRPC services, or cross-language interoperability with proto3
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-protobuf3.svg?label=vajram-protobuf3&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-protobuf3) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-protobuf3-codegen.svg?label=vajram-protobuf3-codegen&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-protobuf3-codegen)
* **Location:** [./vajram/extensions/protobuf/vajram-protobuf3](vajram/extensions/protobuf/vajram-protobuf3) · [./vajram/extensions/protobuf/vajram-protobuf3-codegen](vajram/extensions/protobuf/vajram-protobuf3-codegen)
* **BOM:** [Krystal BOM](#krystal-bom)

**vajram-protobuf2024e** / **vajram-protobuf2024e-codegen**
* **What it has**
  * Protobuf edition 2024 `SerdeProtocol` support and matching codegen
* **When to use**
  * When targeting protobuf edition 2024 features and syntax instead of classic proto3
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-protobuf2024e.svg?label=vajram-protobuf2024e&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-protobuf2024e) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-protobuf2024e-codegen.svg?label=vajram-protobuf2024e-codegen&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-protobuf2024e-codegen)
* **Location:** [./vajram/extensions/protobuf/vajram-protobuf2024e](vajram/extensions/protobuf/vajram-protobuf2024e) · [./vajram/extensions/protobuf/vajram-protobuf2024e-codegen](vajram/extensions/protobuf/vajram-protobuf2024e-codegen)
* **BOM:** [Krystal BOM](#krystal-bom)
* **README:** [vajram-protobuf2024e/README.md](vajram/extensions/protobuf/vajram-protobuf2024e/README.md)

###### GraphQL

**vajram-graphql** / **vajram-graphql-codegen**
* **What it has**
  * Capabilities to execute vajrams by invoking GraphQL queries, plus schema-to-vajram codegen
* **When to use**
  * When exposing or consuming vajram logic through a GraphQL interface
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-graphql.svg?label=vajram-graphql&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-graphql) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-graphql-codegen.svg?label=vajram-graphql-codegen&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-graphql-codegen)
* **Location:** [./vajram/extensions/graphql/vajram-graphql](vajram/extensions/graphql/vajram-graphql) · [./vajram/extensions/graphql/vajram-graphql-codegen](vajram/extensions/graphql/vajram-graphql-codegen)
* **BOM:** [Krystal BOM](#krystal-bom)

###### SQL

**vajram-sql** / **vajram-sql-codegen**
* **What it has**
  * Declarative SQL table and query modelling in Krystal graphs, plus annotation processors for SQL compute vajrams
* **When to use**
  * To model and execute SQL operations as first-class vajrams inside a Krystal graph
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-sql.svg?label=vajram-sql&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-sql) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-sql-codegen.svg?label=vajram-sql-codegen&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-sql-codegen)
* **Location:** [./vajram/extensions/sql/vajram-sql](vajram/extensions/sql/vajram-sql) · [./vajram/extensions/sql/vajram-sql-codegen](vajram/extensions/sql/vajram-sql-codegen)
* **BOM:** [Krystal BOM](#krystal-bom)
* **README:** [vajram-sql/README.md](vajram/extensions/sql/vajram-sql/README.md) · [vajram-sql-codegen/README.md](vajram/extensions/sql/vajram-sql-codegen/README.md)

**vajram-sql-vertx** / **vajram-sql-vertx-codegen**
* **What it has**
  * Vert.x-based SQL execution backend for `vajram-sql`, plus codegen for Vert.x result wrappers
* **When to use**
  * When running SQL vajrams against databases via the Vert.x SQL client
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-sql-vertx.svg?label=vajram-sql-vertx&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-sql-vertx) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-sql-vertx-codegen.svg?label=vajram-sql-vertx-codegen&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-sql-vertx-codegen)
* **Location:** [./vajram/extensions/sql/vertx/vajram-sql-vertx](vajram/extensions/sql/vertx/vajram-sql-vertx) · [./vajram/extensions/sql/vertx/vajram-sql-vertx-codegen](vajram/extensions/sql/vertx/vajram-sql-vertx-codegen)
* **BOM:** [Krystal BOM](#krystal-bom)
* **README:** [vajram-sql-vertx-codegen/README.md](vajram/extensions/sql/vertx/vajram-sql-vertx-codegen/README.md)

###### Resilience

**vajram-resilience4j**
* **What it has**
  * Resilience4j decorators for circuit breakers and bulkheads around vajram invocations
* **When to use**
  * To add fault-tolerance policies to vajram execution without embedding resilience logic in business code
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/vajram-resilience4j.svg?label=vajram-resilience4j&color=green)](https://central.sonatype.com/artifact/com.flipkart.krystal/vajram-resilience4j)
* **Location:** [./vajram/extensions/resilience4j/vajram-resilience4j](vajram/extensions/resilience4j/vajram-resilience4j)

#### Lattice

Lattice is the application framework for packaging vajrams (or other logic) into deployable services.
See [lattice/README.md](lattice/README.md) for concepts like `LatticeApplication`, dopants, and
service exposure.

##### Core

**lattice-core**
* **What it has**
  * Core Lattice runtime — `LatticeApplication`, dopant model, and application container APIs
* **When to use**
  * As the foundation for any Lattice-based service or executable application
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-core.svg?label=lattice-core&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-core)
* **Location:** [./lattice/lattice-core](lattice/lattice-core)
* **BOM:** [Lattice BOM](#lattice-bom)

**lattice-codegen**
* **What it has**
  * Shared codegen infrastructure for all Lattice annotation processors and extensions
* **When to use**
  * As a transitive dependency of Lattice extension codegen modules
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-codegen.svg?label=lattice-codegen&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-codegen)
* **Location:** [./lattice/lattice-codegen](lattice/lattice-codegen)
* **BOM:** [Lattice BOM](#lattice-bom)

##### Lattice extensions

###### Dependency injection

**lattice-cdi** / **lattice-cdi-codegen**
* **What it has**
  * Jakarta CDI as the Lattice dependency-injection framework, plus CDI-specific codegen
* **When to use**
  * When building Lattice apps on CDI (for example, `@LatticeApp(dependencyInjectionFramework = CdiFramework.class)`)
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-cdi.svg?label=lattice-cdi&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-cdi) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-cdi-codegen.svg?label=lattice-cdi-codegen&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-cdi-codegen)
* **Location:** [./lattice/extensions/cdi/lattice-cdi](lattice/extensions/cdi/lattice-cdi) · [./lattice/extensions/cdi/lattice-cdi-codegen](lattice/extensions/cdi/lattice-cdi-codegen)
* **BOM:** [Lattice BOM](#lattice-bom)

**lattice-guice** / **lattice-guice-codegen** / **lattice-guice-servlet**
* **What it has**
  * Guice (and Guice Servlet) as Lattice DI frameworks, plus Guice-specific codegen
* **When to use**
  * When building Lattice apps on Guice instead of CDI, including servlet-hosted deployments
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-guice.svg?label=lattice-guice&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-guice) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-guice-codegen.svg?label=lattice-guice-codegen&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-guice-codegen) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-guice-servlet.svg?label=lattice-guice-servlet&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-guice-servlet)
* **Location:** [./lattice/extensions/guice/lattice-guice](lattice/extensions/guice/lattice-guice) · [./lattice/extensions/guice/lattice-guice-codegen](lattice/extensions/guice/lattice-guice-codegen) · [./lattice/extensions/guice/lattice-guice-servlet](lattice/extensions/guice/lattice-guice-servlet)
* **BOM:** [Lattice BOM](#lattice-bom)

###### gRPC

**lattice-grpc** / **lattice-grpc-codegen**
* **What it has**
  * gRPC service hosting for Lattice apps, plus codegen for gRPC stubs and service bindings
* **When to use**
  * To expose vajrams as gRPC services (pair with a matching `SerdeProtocol` such as protobuf3 or protobuf 2024e)
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-grpc.svg?label=lattice-grpc&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-grpc) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-grpc-codegen.svg?label=lattice-grpc-codegen&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-grpc-codegen)
* **Location:** [./lattice/extensions/grpc/lattice-grpc](lattice/extensions/grpc/lattice-grpc) · [./lattice/extensions/grpc/lattice-grpc-codegen](lattice/extensions/grpc/lattice-grpc-codegen)
* **BOM:** [Lattice BOM](#lattice-bom)

###### REST

**lattice-rest-api** / **lattice-rest** / **lattice-rest-codegen**
* **What it has**
  * REST API contracts, REST service dopant, and codegen that wires vajrams to HTTP endpoints
* **When to use**
  * To turn `@InvocableOutsideProcess` vajrams into REST APIs inside a Lattice application
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-rest-api.svg?label=lattice-rest-api&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-rest-api) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-rest.svg?label=lattice-rest&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-rest) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-rest-codegen.svg?label=lattice-rest-codegen&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-rest-codegen)
* **Location:** [./lattice/extensions/rest/lattice-rest-api](lattice/extensions/rest/lattice-rest-api) · [./lattice/extensions/rest/lattice-rest](lattice/extensions/rest/lattice-rest) · [./lattice/extensions/rest/lattice-rest-codegen](lattice/extensions/rest/lattice-rest-codegen)
* **BOM:** [Lattice BOM](#lattice-bom)

**lattice-rest-quarkus** / **lattice-rest-quarkus-codegen**
* **What it has**
  * Quarkus-specific REST stack integration for Lattice REST services
* **When to use**
  * When hosting Lattice REST APIs on Quarkus (see `rest-json-quarkus-lattice-sample`)
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-rest-quarkus.svg?label=lattice-rest-quarkus&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-rest-quarkus) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-rest-quarkus-codegen.svg?label=lattice-rest-quarkus-codegen&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-rest-quarkus-codegen)
* **Location:** [./lattice/extensions/rest/quarkus/lattice-rest-quarkus](lattice/extensions/rest/quarkus/lattice-rest-quarkus) · [./lattice/extensions/rest/quarkus/lattice-rest-quarkus-codegen](lattice/extensions/rest/quarkus/lattice-rest-quarkus-codegen)
* **BOM:** [Lattice BOM](#lattice-bom)

**lattice-rest-dropwizard** / **lattice-rest-dropwizard-codegen**
* **What it has**
  * Dropwizard-specific REST stack integration for Lattice REST services
* **When to use**
  * When hosting Lattice REST APIs on Dropwizard (see `rest-json-dropwizard-lattice-sample`)
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-rest-dropwizard.svg?label=lattice-rest-dropwizard&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-rest-dropwizard) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-rest-dropwizard-codegen.svg?label=lattice-rest-dropwizard-codegen&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-rest-dropwizard-codegen)
* **Location:** [./lattice/extensions/rest/dropwizard/lattice-rest-dropwizard](lattice/extensions/rest/dropwizard/lattice-rest-dropwizard) · [./lattice/extensions/rest/dropwizard/lattice-rest-dropwizard-codegen](lattice/extensions/rest/dropwizard/lattice-rest-dropwizard-codegen)
* **BOM:** [Lattice BOM](#lattice-bom)

###### GraphQL

**lattice-graphql-rest** / **lattice-graphql-codegen**
* **What it has**
  * GraphQL-over-REST support for Lattice apps, plus GraphQL service codegen
* **When to use**
  * To expose vajrams through GraphQL inside a Lattice application (see `graphql-rest-json-lattice-sample`)
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-graphql-rest.svg?label=lattice-graphql-rest&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-graphql-rest) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-graphql-codegen.svg?label=lattice-graphql-codegen&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-graphql-codegen)
* **Location:** [./lattice/extensions/graphql/rest/lattice-graphql-rest](lattice/extensions/graphql/rest/lattice-graphql-rest) · [./lattice/extensions/graphql/lattice-graphql-codegen](lattice/extensions/graphql/lattice-graphql-codegen)
* **BOM:** [Lattice BOM](#lattice-bom)

###### Quarkus runtime

**lattice-quarkus** / **lattice-quarkus-codegen**
* **What it has**
  * Quarkus runtime integration for Lattice applications, plus Quarkus-specific codegen
* **When to use**
  * As the base Lattice + Quarkus stack for Quarkus-hosted services (REST, MCP, A2A samples)
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-quarkus.svg?label=lattice-quarkus&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-quarkus) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-quarkus-codegen.svg?label=lattice-quarkus-codegen&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-quarkus-codegen)
* **Location:** [./lattice/extensions/quarkus/lattice-quarkus](lattice/extensions/quarkus/lattice-quarkus) · [./lattice/extensions/quarkus/lattice-quarkus-codegen](lattice/extensions/quarkus/lattice-quarkus-codegen)
* **BOM:** [Lattice BOM](#lattice-bom)

###### MCP and A2A

**lattice-mcp** / **lattice-mcp-quarkus-codegen**
* **What it has**
  * Model Context Protocol (MCP) server support for Lattice apps, plus Quarkus MCP codegen
* **When to use**
  * To expose Lattice vajrams as MCP tools (see `mcp-quarkus-lattice-sample`)
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-mcp.svg?label=lattice-mcp&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-mcp) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-mcp-quarkus-codegen.svg?label=lattice-mcp-quarkus-codegen&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-mcp-quarkus-codegen)
* **Location:** [./lattice/extensions/mcp/lattice-mcp](lattice/extensions/mcp/lattice-mcp) · [./lattice/extensions/mcp/quarkus/lattice-mcp-quarkus-codegen](lattice/extensions/mcp/quarkus/lattice-mcp-quarkus-codegen)
* **BOM:** [Lattice BOM](#lattice-bom)

**lattice-a2a** / **lattice-a2a-codegen**
* **What it has**
  * [Agent-to-Agent (A2A)](https://google.github.io/A2A/specification/) server support for Lattice apps, plus A2A codegen
* **When to use**
  * To build A2A-compatible agent servers on Lattice (see `a2a-quarkus-lattice-sample`)
* **Coordinates:** [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-a2a.svg?label=lattice-a2a&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-a2a) · [![Maven Central](https://img.shields.io/maven-central/v/com.flipkart.krystal/lattice-a2a-codegen.svg?label=lattice-a2a-codegen&color=blue)](https://central.sonatype.com/artifact/com.flipkart.krystal/lattice-a2a-codegen)
* **Location:** [./lattice/extensions/a2a/lattice-a2a](lattice/extensions/a2a/lattice-a2a) · [./lattice/extensions/a2a/lattice-a2a-codegen](lattice/extensions/a2a/lattice-a2a-codegen)
* **BOM:** [Lattice BOM](#lattice-bom)
* **README:** [lattice/extensions/a2a/README.md](lattice/extensions/a2a/README.md)

## Preface

This README gives a high level view of the krysal project.
To understand the basics of krystal. It's important to go through two more documents:

* [Vajram README](./vajram/vajram-java-sdk/README.md)
* [Krystex README](./krystex/README.md)

## Introduction

**Super-charging the development of synchronous and asynchronous business workflows.**

The Krystal Project facilitates a developer-friendly way to write high-performant, complex, business
logic which is easily maintainable and observabe. The project encompasses the following major
components.

* Vajram: A programming model which allows developers to design and write code for synchronous
  scatter gather business logic in a 'bottom-up' (choreographed) manner.
* Krystex: A runtime environment which executes synchronous parts of the code written in vajram
  programming model in an optimal way by understanding static dependencies between pieces of code,
  creating a logical Directed-Acyclic-Graph, and executing the DAG with maximal concurrency.
* Honeycomb: An asynchronous workflow orchestrator which orchestrates the asynchronous parts of
  worflows written in the vajram programming model.

## Design Goals

Components of the Krystal Project adhere to the following design goals:

1. Separation of functional contracts and non-functional implementation details of dependencies:
   Developers coding a
   piece of functional business logic which depends on some other piece of business logic should be
   completely shielded
   from non-functional implementation details of the dependency as well as the runtime details of
   the environment in
   which the business logic is executing. Here non-functional requirements include:
    * Session-level Caching
    * Concurrency mode (multithreaded thread-per-task model, vs. reactive vs. hybrid model)
    * Batching and batch size of dependency inputs
2. Minimize lateral and upward impact of code changes: If all the business logic executing to
   fulfill a request is seen
   as a Directed acyclic graph, a code change being made in a kryon in the graph should not impact
   the implementation of
   any other kryon in the graph which is a direct descendant of it.
3. Zero glue code: If there is business logic A and business logic B, all the relevant business
   logic should be
   contained within the artefact which represents each piece of business logic.
4. Out-of the box Non-intrusive batching of service-call inputs
5. Optimal end-to-end execution: This programming model is designed to be adopted for executing
   complex business logic
   in systems that power features and user experiences which directly are accessed by end users of
   websites with heavy traffic, thus
   strongly needing a low-latency, high-throughput execution runtime.
    * Minimize use of native OS threads
    * Avoid the possibility of developers erroneously blocking on thread-blocking-code pre-maturely.
    * Streaming-over-network-connection capable
6. Avoid unnecessary bottlenecks and latency long poles: ...
7. Developer friendliness:
    * Minimal bootstrapping overhead
    * Dependency discovery
    * Code generation
    * Single point of coding - One-to-one atomic mapping of coding units and functional
      requirements.
    * Minimize mental-overhead and avoid antipatterns inherent to reactive coding
    * Programming-language-native developer experience: For example, type safety.
8. Observability - application owners get this Out of the Box:
    * Circuit Breaking
    * Live Service Call Dashboards
    * Degradation levers
    * Metrics
    * Logging
    * DAG visualization of a request
9. Testability
    * Backward-incompatibility detection
    * Mocking-out-of-the box
    * Declarative unit test definition
    * Auto-generated unit test code/templates
10. Programming language agnostic spec definitions: Although the project currently supports the java
    programming language, keeping
    the programming-model spec language-agnostic allows the programming model to have
    implementations in different
    languages and allows application owners/feature-developers to choose a language of their choice.

## Krystal developer FAQs

## Contribution

#### Code formatting

All java code should be formatted using
the [google-java-formatter](https://plugins.jetbrains.com/plugin/8527-google-java-format). Building the code autoformats the code.
To formatting code in the IDE, use:
* [Intellij plugin](https://github.com/google/google-java-format#intellij-android-studio-and-other-jetbrains-ides)
* [Eclipse Plugin](https://github.com/google/google-java-format#eclipse)

#### How to bump up to Krystal framework version?

Follow the below-mentioned steps for the Krystal version bump -

1. Build and publish vajram-codegen with new version to local maven repo. Ensure the vajram-codegen
   dependencies version should not be updated.
2. Update the new version in Krystal project build.gradle file and revert the vajram-codegen version
   to previous version. Do a complete build and publish to local maven repo.
3. Update the vajram-codegen to the new version.

Example : Need to update version from 1.6 to 1.7

1. vajram-codegen (build.gradle) update
    - update version from 1.6 to 1.7
    - Build and `gradle publishToMavenLocal` in krystal root directory
2. krystal (build.gradle) update
    - update version from 1.6 to 1.7
    - set `classpath 'com.flipkart.krystal:vajram:'+ project.krystal_version` in vajram-codegen's
      buildscript block to `classpath 'com.flipkart.krystal:vajram:1.6'`
    - Build and `gradle publishToMavenLocal` in krystal root directory
3. Final update
    - revert `classpath 'com.flipkart.krystal:vajram:1.6'+ project.krystal_version` in
      vajram-codegen's buildscript block
      to `classpath 'com.flipkart.krystal:vajram:'+ project.krystal_version`
    - Build and `publishToMavenLocal` and `publish` in krystal root directory
