# Krystex

Krystex (**Kryst**al **Ex**ecutor) is a generic execution engine for synchronous workflows.

## Introduction

Krystex is designed to execute arbitrary synchronous business workflows which can involve making API
network calls to other microservices.

These synchronous workflows could be the business logic inside services which perform
high-throughput, low-latency "scatter-gather" operations from across multiple
other microservices, while performing complex transformations and aggregations of data, before
finally returning the aggregated data as response to their clients.

Synchronous workflows could also be the synchronous parts of large asynchronous workflows which are
made from multiple synchronous "sub-workflows" tied together via asynchronous channels like
persistent queues, timers, end-user responses, etc.

## Design goals

### What krystex is

Krystex is an execution engine which focuses on the non-functional / runtime aspects of the
application. Its concerns include:

* Concurrency
* Low-latency
* High-throughput
* Caching
* Dynamic configurability
* Optimal resource reclamation and garbage collection

### What krystex is NOT

Krystex is not designed to solve for functional and development-time use cases. For example, krystex
**DOES NOT** understand or have support for the following:

* **Mandatory vs. optionality** - For krystex, everything is optional
* **Type-safety** - For krystex, everything is either a plain old java `Object`, a `Throwable` or
  a `HashMap<>` (more on this later)
* **Batching** - For krystex, every unit of computation takes `1` set of inputs and returns
  exactly `1` outputs.
  (This is a bit debatable. One could argue that batching is a non-functional
  aspect of a workflow. In the future, krystex might provide first-class support for batching. But
  for now, batching is expected to be solved by clients of Krystex
  via [LogicDecorators](#logic-decorator).)

Because of this design choice, Krystex is not very developer-friendly, and is not supposed to be
used by application developers directly. It is expected that other
frameworks (like [vajram](../vajram/README.md) and [caramel](../caramel/README.md)) will create
abstractions over krystex which provide a developer-friendly environment to write application logic,
and then translate/compile these abstractions into Krystal native entities for runtime execution.

## Concepts

### KrystalExecutor

The [KrystalExecutor interface](src/main/java/com/flipkart/krystal/krystex/KrystalExecutor.java) is
the only entry point for all Krystex execution requests. The default and, currently, the only
implementation of KrystalExecutor is
the [KrystalNodeExecutor](src/main/java/com/flipkart/krystal/krystex/node/KrystalNodeExecutor.java).
The remaining part of this document focuses on the KrystalNodeExecutor and its internals.

### Node

A node is the atomic unit of work in a krystex. It
has [inputs](#node-input), [dependencies](#node-dependency), [resolvers](#dependency-input-resolver)

### NodeDefinition

A node definition represents the definition of one unit of work in krystex and holds the following
information

* a nodeId - a unique identifier for every node. In a given KrystalNodeExecutor, there can only be
  on instance of a node having a given nodeId.
* a reference to a stateless logic/function (the unit of work) - called
  the [main logic](#node-main-logic) of the node
* references to other node definitions which are [dependencies](#node-dependency) of this node
  definition
* references to [resolver functions](#dependency-input-resolver)

As you can see, none of the above information is request-specific, since there are no references to
inputs and outputs. For this reason, node definitions are created once and cached for the lifecycle
of the application. And are used as templates for [Nodes](#node) which are created afresh for every
new instance of the KrystalNodeExecutor.

### Node Main Logic

Every node has exactly one function which has the responsibility of computing the output of the
node. This called the main logic. If the main logic fails with an Exception, then the node is
considered to have failed with the same exception.

### Node Input

(or just **input**)

* A node can optionally declare inputs.
* Every input has a name which must not clash with the name of any other input
  or [dependency](#node-dependency) of this node.
* A node can complete execution only after values for all its
  inputs are made available to it. Clients of the node (either via KrystalNodeExecutor or other
  nodes
  which [depend](#node-dependency) on this node) are expected to provide one value each for EVERY
  input of the node - only then the node is executed. The provided values could potentially be null
* A node can be executed multiple times with different sets of inputs.
* All the inputs need not be provided to a node together. Different input values can be provided to
  the node at different points in time. The node will greedily perform any intermediate operations (
  See [resolvers](#dependency-input-resolver)) that it can with the provided inputs. The node will
  complete execution only once values for all the inputs are provided. Till then the node will
  remain in an intermediate (semi-executed) state.

### Node Dependency

(or just **dependency**)

* Nodes can optionally declare dependencies, which are nodes on which this node depends.
* Every dependency has a name which must not conflict with the name of any
  other [input](#node-input) or dependency of this node.
* A node can complete execution only after all of its dependencies have completed execution (either
  successfully, with an error, or explicitly skipped.)

### Dependency-Input resolver logic

(or just **resolver**)

* A resolver in a node is a function that is responsible for computing (resolving) the inputs of a
  dependency of a node.
* One resolver can resolve the inputs of exactly one dependency.
* One resolver can resolve one or more inputs of a dependency. A resolver can resolve a subset of
  the inputs of a dependency. In such a scenario, more than one resolver is needed to completely
  resolve that dependency.
* A node which has `N` dependencies, must have at least `N` resolvers - one for each dependency of
  the node (This happens when every resolver resolves all the inputs of its corresponding
  dependency).
* Every input of every dependency must be resolved by exactly one resolver. In other words, all the
  resolvers of a node must together resolve ALL the inputs of all the dependencies of the
  node. Any input of any dependency must not be left unresolved.

### Logic Decorator

<!--TODO-->

## How KrystalNodeExecutor Works

### Command Queue - (SingleThreadExecutor)

The KrystalNodeExecutor executes all the required nodes in a single thread using an event loop
model. This means that none of the resolver logics or main logics of any node are allowed to block
any amount of time. They are expected to return instantly after wrapping any long-running operation
in a `CompletableFuture`.
<!-- TODO add link to workflow diagram -->

### Common data structures

<!--TODO add links to krystal-common data structures-->

### Example execution

<!-- TODO add execution animation -->

## Capabilities

### Recursive dependencies

