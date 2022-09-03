# The Krystal Project

///// This doc is a WIP

Super-charging the development of synchronous and asynchronous business workflows.

## Introduction

The Krystal Project facilitates a developer-friendly way to write high-performant, complex, business logic which is easily maintainable and observabe. The project encompasses the following major components. 
   * Vajram: A programming model which allows developers to design and write code for synchronous scatter gather business logic in a 'bottom-up' manner.
   * Caramel: A programming model which allows developers to design and write code for synchronous and asynchronous workflows in a 'top-down' manner.
   * Krystex: A runtime environment which executes synchronous parts of the code written in vajram/caramel programming models in an optimal way by understanding static dependencies between pieces of code, creating a logical Directed-Acyclic-Graph, and executing the DAG with maximal concurrency.
   * Raven: An asynchronous workflow orchestrator which orchestrates the asynchronous parts of worflows written in the caramel programming model.

## Design Goals

Components of the Krystal Project try to adhere to the following design goals:

1. Separation of functional contracts and non-functional implementation details of dependencies: Developers coding a
   piece of functional business logic which depends on some other piece of business logic should be completely shielded
   from non-functional implementation details of the dependency as well as the runtime details of the environment in
   which the business logic is executing. Here non-functional requirements include:
    * Session-level Caching
    * Concurrency mode (multithreaded thread-per-task model, vs. reactive vs. hybrid model)
    * Batching and batch size of dependency inputs
1. Minimize lateral and upward impact of code changes: If all the business logic executing to fulfill a request is seen
   as a Directed acyclic graph, a code change being made in a node in the graph should not impact the implementation of
   any other node in the graph which is a direct descendant of it.
1. Zero glue code: If there is business logic A and business logic B, all the relevant business logic should be
   contained within the artefact which represents each piece of business logic.
1. Out-of the box Non-intrusive batching of service-call inputs
1. Optimal end-to-end execution: This programming model is designed to be adopted for executing complex business logic
   in systems that power features and user experiences which directly are accessed by end users of websites with heavy traffic, thus
   strongly needing a low-latency, high-throughput execution runtime.
    * Minimize use of native OS threads
    * Avoid the possibility of developers erroneously blocking on thread-blocking-code pre-maturely.
    * Streaming-over-network-connection capable
1. Avoid unnecessary bottlenecks and latency long poles: ... 
1. Developer friendliness:
    * Minimal bootstrapping overhead
    * Dependency discovery
    * Code generation
    * Single point of coding - One-to-one atomic mapping of coding units and functional requirements.
    * Minimize mental-overhead and avoid anti patterns inherent to reactive coding
    * Programming-language-native developer experience: For example, type safety.
1. Observability - application owners get this Out of the Box:
    * Circuit Breaking
    * Live Service Call Dashboards
    * Degradation levers
    * Metrics
    * Logging
    * DAG visualization of a request
1. Testability
    * Backward-incompatibility detection
    * Mocking-out-of-the box
    * Declarative unit test definition
    * Auto-generated unit test code/templates
1. Programming language agnostic spec definitions: Although the project currently supports the java programming language, keeping
    the programming-model spec language-agnostic allows the programming model to have implementations in different
    languages and allows application owners/feature-developers to choose a language of their choice.
