# The Krystal Project

## Latest Releases

Single Bill-Of-Materials artefact with all krystal artefacts versions defined.  
[![Clojars Project](https://img.shields.io/clojars/v/com.flipkart.krystal/krystal-bom.svg)](https://clojars.org/com.flipkart.krystal/krystal-bom)

#### Included artefacts:

1. `com.flipkart.krystal:krystal-common`: Contains all common data models and classes used across
   krystal project
2. `com.flipkart.krystal:vajramDef-java-sdk`: Contains the sdk for writing vajrams. Application
   developers will generally use clasess of this artefact when writing business logic.
3. `com.flipkart.krystal:vajramDef-codegen`: Contains the annotation processors and krystal gradle
   plugin. Developers add this artefact as an annotation processor dependency in their gradle build
   file, and add the `com.flipkart.krystal` plugin to their gradle project.
    1. The annotation processors generate the vajramDef models and impls during compilation phase.
    2. The krystal plugin adds `codegenVajramModels` gradle task and adds it as a dependency to
       the `compileJava` task so that models are code generated on every compile. It also configures
       the `codegenVajramModels` task to run the annotation processor which generates the models and
       the `compileJava` task to generate the vajramDef impl files using the models generated in the
       previous step. The plugin artefact's coordinates
       are `com.flipkart.krystal:com.flipkart.krystal.gradle.plugin`, but developers generally don't
       have to mention this in their code. Adding `com.flipkart.krystal` plugin will pull this
       artefact
4. `com.flipkart.krystal:krystex`: (**Kryst**al **Ex**ecutor) Contains the execution engine which
   executes the synchronous orchestration call graph at runtime.
5. `com.flipkart.krystal:vajramDef-krystex`: This module acts as a "transpiler" which tranlates
   the `vajramDef-java-sdk` based developer-written "vajramDef"s to runtime-executable "kryon" models
   which the `krystex` module understands. This allows `vajramDef` and `krystex` modules to be
   independant and agnostic of each other. Classes common to both vajramDef and krystex modules are
   in `krystal-common` module.
6. `com.flipkart.krystal:vajramDef-guice`: This module contains a kryon decorator which allows
   the `@Inject` facetValues in vajrams to be provided by the guice injector. See `vajramDef-samples` module
   for how to use this decorator.

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
* Krystex: A runtime environment which executes synchronous parts of the code written in vajramDef
  programming model in an optimal way by understanding static dependencies between pieces of code,
  creating a logical Directed-Acyclic-Graph, and executing the DAG with maximal concurrency.
* Honeycomb: An asynchronous workflow orchestrator which orchestrates the asynchronous parts of
  worflows written in the vajramDef programming model.

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

1. Build and publish vajramDef-codegen with new version to local maven repo. Ensure the vajramDef-codegen
   dependencies version should not be updated.
2. Update the new version in Krystal project build.gradle file and revert the vajramDef-codegen version
   to previous version. Do a complete build and publish to local maven repo.
3. Update the vajramDef-codegen to the new version.

Example : Need to update version from 1.6 to 1.7

1. vajramDef-codegen (build.gradle) update
    - update version from 1.6 to 1.7
    - Build and `gradle publishToMavenLocal` in krystal root directory
2. krystal (build.gradle) update
    - update version from 1.6 to 1.7
    - set `classpath 'com.flipkart.krystal:vajram:'+ project.krystal_version` in vajram-codegen's
      buildscript block to `classpath 'com.flipkart.krystal:vajram:1.6'`
    - Build and `gradle publishToMavenLocal` in krystal root directory
3. Final update
    - revert `classpath 'com.flipkart.krystal:vajram:1.6'+ project.krystal_version` in
      vajramDef-codegen's buildscript block
      to `classpath 'com.flipkart.krystal:vajram:'+ project.krystal_version`
    - Build and `publishToMavenLocal` and `publish` in krystal root directory
