# Lattice A2A Extension

This directory contains the Lattice extension modules that enable building
[A2A (Agent-to-Agent)](https://google.github.io/A2A/specification/) servers using
the Krystal Lattice framework.

---

## High-Level Design & Philosophy

### Core Philosophy

Lattice treats every protocol integration as a *dopant* — a capability added to a
`LatticeApplication` without coupling business logic to protocol concerns. The A2A extension
follows the same pattern used by the MCP and REST extensions:

1. **Annotation-driven declaration** — developers annotate their `LatticeApplication` subclass
   with `@A2AServer` and `@Agent` to declare which Vajrams serve as the execution and
   cancellation handlers for each agent skill.

2. **Code-generated adapter layer** — at compile time, an annotation processor
   (`lattice-a2a-codegen`) reads the `@A2AServer` annotation and generates a concrete
   `AgentExecutor` CDI bean. This bean bridges the A2A protocol layer to the Vajram execution
   engine without any runtime reflection.

3. **Separation of concerns** — business logic lives in Vajrams, which know nothing about A2A.
   The generated adapter handles all protocol mapping: extracting text from `TextPart` objects,
   routing to the correct skill Vajram, invoking `AgentEmitter` to signal task state transitions.

4. **User-controlled `AgentCard`** — the agent card (identity, capabilities, skills) is declared
   as a regular CDI producer bean by the application author. The A2A Java SDK automatically serves
   it at `/.well-known/agent.json`. This keeps the framework unopinionated about agent metadata.

---

## Module Structure

```
lattice/extensions/a2a/
├── lattice-a2a/            Core extension: annotations + A2AServerDopant
└── lattice-a2a-codegen/    Compile-time code generator (annotation processor)
```

### `lattice-a2a`

Contains:

| Class | Role |
|---|---|
| `@A2AServer` | Marks a `LatticeApplication` as an A2A server; holds the list of `@Agent` entries |
| `@Agent` | Declares one agent skill: name, executor Vajram, optional canceller Vajram |
| `A2AServerDopant` | Dopant that delegates Vajram execution to `KrystexDopant` |
| `A2AServerDopantSpec` | Spec / builder for `A2AServerDopant`; referenced in `@DopeWith` methods |

### `lattice-a2a-codegen`

Contains `QuarkusA2ACodegenProvider`, a `LatticeCodeGeneratorProvider` (SPI loaded via
`@AutoService`) that generates a class named `$AppName$_QuarkusA2AAgentExecutor` for each
`LatticeApplication` annotated with `@A2AServer`. This generated class:

- implements `org.a2aproject.sdk.server.agentexecution.AgentExecutor`
- is a `@Singleton` CDI bean (automatically overrides the `@DefaultBean` in the A2A SDK)
- routes `execute()` and `cancel()` calls to the appropriate Vajram via `A2AServerDopant`

---

## Request Context Mapping (Input Facet Conventions)

The codegen maps input facets of executor / canceller Vajrams from the incoming
`RequestContext` using the following **name conventions**:

| Facet name | `RequestContext` method |
|---|---|
| `userInput`, `input`, `message`, `text` | `getUserInput()` — text of the first `TextPart` |
| `taskId` | `getTaskId()` |
| `contextId` | `getContextId()` |

Facets that do not match any convention are left unset. If such a facet is annotated
`@IfAbsent(FAIL)` a runtime error will be produced, which is the intended signal that
the Vajram requires an input the A2A context cannot supply.

For canceller Vajrams, `userInput`-group facets are **not** mapped (cancellation does not
carry user message text); only `taskId` and `contextId` are mapped.

---

## Multi-Agent Skill Routing

When `@A2AServer` declares multiple `@Agent` entries, the generated executor routes
each incoming task based on the **`skillId` key in `MessageSendParams.metadata()`**:

```java
@A2AServer(agents = {
  @Agent(name = "echo",    executor = EchoAgent.class,   canceller = EchoAgentCanceller.class),
  @Agent(name = "reverse", executor = ReverseAgent.class)
})
```

A client selects a skill by including `"skillId": "reverse"` in the request metadata.
If no `skillId` is present, the **first declared agent** is used as the default.

---

## AgentCard

The `AgentCard` must be provided as a CDI producer bean:

```java
@ApplicationScoped
public class MyAgentCardProducer {

  @Produces
  @ApplicationScoped
  public AgentCard agentCard() {
    return AgentCard.builder()
        .name("My Agent")
        .version("1.0.0")
        .capabilities(AgentCapabilities.builder().streaming(false).build())
        .skills(List.of(
            AgentSkill.builder().id("echo").name("Echo").build()))
        .build();
  }
}
```

The A2A Java SDK (via `DefaultProducers`) provides a `@DefaultBean` fallback, so
omitting the producer will not break compilation — but it will serve an incomplete card.

---

## Runtime Response Flow

```
HTTP POST /               (A2A SDK → Quarkus → A2AServerRoutes)
    │
    ▼
AgentExecutor.execute(RequestContext, AgentEmitter)   [generated class]
    │ emitter.submit()       ← immediately acknowledge
    │
    ▼
A2AServerDopant.executeAgent(ImmutableRequest)
    │
    ▼
KrystexDopant.executeRequest(...)    ← Vajram graph execution
    │
    ▼
whenComplete: emitter.complete(message)  or  emitter.fail(message)
```

---

## Quick Start

See [`lattice/samples/a2a/quarkus/a2a-quarkus-lattice-sample`](../../samples/a2a/quarkus/a2a-quarkus-lattice-sample/)
for a working example with two skills (echo + reverse) and end-to-end tests.
