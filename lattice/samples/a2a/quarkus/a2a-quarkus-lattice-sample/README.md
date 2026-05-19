# A2A Quarkus Lattice Sample

A working example of a multi-skill A2A server built with the Krystal Lattice framework and
powered by Quarkus + the A2A Java SDK.

---

## What This Sample Demonstrates

| Concept | Implementation |
|---|---|
| Two-skill A2A server | `@A2AServer` with `echo` and `reverse` `@Agent` entries |
| Executor Vajram | `EchoAgent` (returns `"Echo: <input>"`), `ReverseAgent` (reverses text) |
| Canceller Vajram | `EchoAgentCanceller` (confirms cancellation by task ID) |
| No-canceller agent | `ReverseAgent` — no `canceller` declared; runtime calls `emitter.cancel()` directly |
| Custom `AgentCard` | `SampleAgentCardProducer` — produces the card served at `/.well-known/agent.json` |
| Multi-skill routing | Client sends `"metadata": {"skillId": "reverse"}` to select the reverse skill |

---

## Running the Server

```bash
./gradlew :lattice:samples:a2a:quarkus:a2a-quarkus-lattice-sample:quarkusDev -PunsafeCompile=true
```

The agent card will be available at:
```
GET http://localhost:8080/.well-known/agent-card.json
```

Send a task to the echo skill:
```bash
curl -X POST http://localhost:8080/ \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "message/send",
    "params": {
      "message": {
        "role": "user",
        "parts": [{"kind": "text", "text": "Hello A2A!"}]
      },
      "metadata": {"skillId": "echo"}
    }
  }'
```

Send a task to the reverse skill:
```bash
curl -X POST http://localhost:8080/ \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "id": "2",
    "method": "message/send",
    "params": {
      "message": {
        "role": "user",
        "parts": [{"kind": "text", "text": "Hello"}]
      },
      "metadata": {"skillId": "reverse"}
    }
  }'
```

---

## Running Tests

```bash
./gradlew :lattice:samples:a2a:quarkus:a2a-quarkus-lattice-sample:test -PunsafeCompile=true
```

Tests start an embedded Quarkus server on port 18085 and exercise:

- `GET /.well-known/agent.json` — agent card content
- `POST /` with `skillId: echo` — echo response
- `POST /` with `skillId: reverse` — reversed text response
- `POST /` without `skillId` — defaults to first skill (echo)

---

## Project Structure

```
src/main/java/
  logic/
    EchoAgent.java            Executor Vajram for the "echo" skill
    EchoAgentCanceller.java   Canceller Vajram for the "echo" skill
    ReverseAgent.java         Executor Vajram for the "reverse" skill (no canceller)
  QuarkusA2AServer.java       LatticeApplication declaration (@A2AServer)
  SampleAgentCardProducer.java CDI producer for AgentCard

src/test/java/
  QuarkusA2AServerE2eTest.java  End-to-end integration tests
```
