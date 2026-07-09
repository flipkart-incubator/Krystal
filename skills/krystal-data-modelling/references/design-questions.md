# Design questions to deliberate before writing a `@ModelRoot`

Most `@ModelRoot` parameters aren't just codegen switches — they're design decisions that ripple into the
generated code's shape, which protocols the model can ever support, and how safely it can evolve later. Several
are effectively **one-way doors**: reversing them after other code depends on the model is a breaking change,
not a quick edit. Ask the user about each question below that applies (skip the ones that obviously don't, e.g.
skip protocol questions if the model is enum-only with a single caller in-process) — state the options, their
consequences, and your recommendation, rather than silently picking a default. If the user has no strong
opinion, pick the option in **bold** below as the sensible default and say so explicitly, so it can be
corrected.

Each question links back to `references/annotations.md` for the exact mechanics/compile-time rules, and to the
framework's own source/doc on GitHub for the authoritative definition.

## 1. Does this need to be a brand-new `@ModelRoot` at all?

**Ask:** "Should this be a new model, should it extend/reuse an existing one, or is a plain Java `record`
enough (no serde, no boundary crossing)?"

| Option | Meaning | Pros | Cons |
|---|---|---|---|
| **New `@ModelRoot`** | A dedicated interface/enum for this data shape. | Clean isolation; `type`/`pure`/protocols tuned exactly for this use; no risk of an unrelated model growing fields it doesn't need. | Some field duplication if a very similar model already exists. |
| Reuse/extend an existing `@ModelRoot` | Add fields to, or nest, an existing model. | Less duplication, one source of truth. | Couples unrelated call sites to the same model; risks a `type`/`pure` mismatch if the existing model wasn't designed for this new use (e.g. adding a REQUEST-only field to a model already used as a RESPONSE). |
| Plain Java `record` (no `@ModelRoot`) | No codegen at all. | Zero annotation-processing overhead; simplest option for a value that never crosses a boundary. | No builder, no serde wrapper, can't be nested inside another `@ModelRoot`, can't be used as a Vajram input/output. |

Reference: [`ModelRoot.java`](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/src/main/java/com/flipkart/krystal/model/ModelRoot.java).

## 2. What is this model's role — `type`?

**Ask:** "Is this model only ever built by a client to send data in (**REQUEST**), only ever returned to a
client (**RESPONSE**), used as both, or a general-purpose value object with no request/response semantics?"

| Option | Meaning | Pros | Cons |
|---|---|---|---|
| `REQUEST` | Accepts data from a client. | Implicit `@IfAbsent` default is `MAY_FAIL_CONDITIONALLY` — matches lenient, partially-specified client input well. | Cannot be nested inside a `RESPONSE`-only model — if you later need to also return this shape to a client, you must widen `type`, which is a real (if usually safe) API-shape change. |
| `RESPONSE` | Returned to a client. | Implicit `@IfAbsent` default is `FAIL` — the server-side contract "this field is always populated" is explicit and safe by default. | `MAY_FAIL_CONDITIONALLY` is a compile error here — every conditionally-available field must be modelled as `WILL_NEVER_FAIL`/`ASSUME_DEFAULT_VALUE` instead, which is less expressive for "usually present, occasionally not, and here's why" documentation. |
| `{REQUEST, RESPONSE}` (dual) | Serves both directions (e.g. a shared `Address` type sent in a request and echoed back in a response). | One model instead of two near-duplicates for a symmetric shape. | **No implicit `@IfAbsent` default — every field needs an explicit annotation**, and each field must satisfy both rule sets at once (e.g. a `WILL_NEVER_FAIL` field must still be `Optional`/`@Nullable`, and `MAY_FAIL_CONDITIONALLY` is allowed only because the model is also a REQUEST). More upfront annotation work. |
| **`{}` (general-purpose, the default)** | No request/response semantics. | Nestable inside *any* other model regardless of its `type`; simplest choice when the model is a pure value object (e.g. `Address`, `Money`) with no server-only or client-only fields. | Doesn't self-document intended direction, so a field that should really only ever be server-computed (and thus arguably `RESPONSE`-only) can slip into a request path without a compile-time nudge. |

Reference: [`Krystal-models.md` § `@ModelRoot` Annotation](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/Krystal-models.md#modelroot-annotation)
and § Nested Model Type Consistency; `references/annotations.md` Step 4.

## 3. Should this model be pure (`pure`)?

**Ask:** "Do every one of this model's fields stay within Krystal-native types (primitives, `String`,
`PrimitiveArray`, other pure `@ModelRoot` models/enums, `List`/`Map` of these) — or does it genuinely need to
reference a type outside the Krystal model system (a third-party SDK type, a domain object, `java.time.*`,
etc.)?"

| Option | Meaning | Pros | Cons |
|---|---|---|---|
| **`pure = true` (default)** | Fields restricted to the closed, serde-friendly type set. | Required to ever support `Protobuf3`/`Fory`; forces a clean value-object shape with no accidental leakage of internal implementation types into the wire contract. | Every "external" type must be wrapped/represented via primitives, `String`, or a nested pure model — more upfront modelling effort. |
| `pure = false` | Lifts the type restriction entirely. | Lets a field be any Java type — needed for a model that intentionally wraps a non-Krystal type (this is also why auto-generated `REQUEST` models default to `pure = false`). | **This model can never support `Protobuf3` or `Fory`** (compile error if you try) — a real, hard-to-reverse constraint on which protocols this model can ever gain later. |

Reference: [`Krystal-models.md` § Model Purity](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/Krystal-models.md#model-purity);
`references/annotations.md`'s Model Purity section (cites `SerdeModelValidator`'s actual compile-time check).

## 4. Which wire protocols does this model actually need (`@SupportedModelProtocol`)?

**Ask:** "Where does this model's data go — kept in-process only, exposed over a JSON REST API, exposed over
gRPC/Protobuf, or used for high-throughput same-JVM/cross-JVM serialization via Fory? List every protocol this
model genuinely needs — each one is extra codegen surface and extra constraints (see Purity above), not free
capability to add 'just in case'."

| Protocol | Generated class | Pros | Cons |
|---|---|---|---|
| **`PlainJavaObject`** | `_ImmutPojo` | Always safe to add; needed for the in-memory representation used by tests/mocks and any purely in-process consumer. | Not usable across a real process boundary — no actual serialization. |
| `Json` | `_ImmutJson` | Human-readable payloads (easy to debug/log); broad ecosystem interop (any HTTP client); doesn't require purity; forgiving evolution (unknown fields ignored on read, unknown enum values fall back to `UNKNOWN`). | Larger payloads and slower ser/de than a binary protocol; no compile-time client-side contract the way a `.proto`-generated stub gives you. |
| `Protobuf3` | `_ImmutProto` (+ generated `.proto` schema) | Compact binary wire format; real cross-language schema (`.proto` file) other services/languages can consume; `@SerialId` gives an explicit, auditable field-evolution story. | **Requires `pure = true`** (compile error otherwise); requires committing to the `@SerialId` all-or-none discipline (see Q5); harder to eyeball a raw payload while debugging; adds a protoc/gRPC toolchain dependency to the build. |
| `Fory` | `_ImmutFory` | Very fast, JIT-compiled, no IDL/schema file needed; good for internal same-language (JVM-to-JVM) caches, queues, or IPC where every consumer is also Java/Krystal. | **Also requires `pure = true`**; no cross-language guarantee (unlike Protobuf); schema evolution is managed by the application, not enforced by the framework, so renaming/removing a field is riskier than Protobuf's designed evolution story. |

Reference: [`Krystal-models.md` § Serde Implementations / § @SupportedModelProtocol](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/Krystal-models.md);
[`Fory.java`](https://github.com/flipkart-incubator/Krystal/blob/main/vajram/extensions/fory/vajram-fory/src/main/java/com/flipkart/krystal/vajram/fory/Fory.java)'s
class doc for the Fory trade-offs specifically; `references/annotations.md`'s `@SupportedModelProtocol` section
(also documents the real repeated-annotation syntax).

## 5. Does this model need stable binary field indices now (`@SerialId`)?

**Ask:** "Will this model ever need a binary protocol (`Protobuf3`/`Fory`) — now, or plausibly later — and
might its fields ever be reordered, inserted in the middle, or removed?"

| Option | Meaning | Pros | Cons |
|---|---|---|---|
| Add `@SerialId` to every field now | Pin an explicit wire index per field upfront. | Locks in a stable wire contract before you actually need a binary protocol; safe to reorder fields in the source file later without silently changing the wire format; painless to add `Protobuf3`/`Fory` retroactively. | Extra bookkeeping — must pick and track unique indices for every field (all-or-none rule); slightly more verbose model. |
| **Skip `@SerialId` (rely on declaration order)** | Binary index defaults to ordinal/declaration order. | Less boilerplate — fine for a model that will only ever be `PlainJavaObject`/`Json`. | If a binary protocol is added later, or fields are reordered/inserted before that point, the implicit index shifts silently. Retrofitting `@SerialId` at that point requires auditing and pinning every field's index by hand to avoid a breaking wire-format change. |

This is genuinely all-or-none per model — you cannot use it on "just the fields you're worried about."

Reference: [`Krystal-models.md` § `@SerialId`](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/Krystal-models.md#serialid);
[`SerialId.java`](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/src/main/java/com/flipkart/krystal/serial/SerialId.java).

## 6. Should the builder double as the model type (`builderExtendsModelRoot`)?

**Ask:** "Does anything need to accept this model's `Builder` anywhere the finished model type is expected —
e.g. a partially-built response threaded through a pipeline/Vajram before a final `_build()` call?"

| Option | Meaning | Pros | Cons |
|---|---|---|---|
| `builderExtendsModelRoot = true` | Generated `Builder` also implements the model root interface. | An in-progress `Builder` can be passed/injected anywhere the model type is expected, with no premature `_build()`; useful for incremental/streaming construction (e.g. a REST framework injecting a partially-built response builder into a Vajram — see `InnerDataV2`/`SubMessage` in `references/examples.md`). | **Not allowed on enum model roots** (compile error); blurs "still being built" vs. "finished", so relying on `equals()`/`hashCode()`/`toString()` on a not-yet-fully-built `Builder` can be surprising. |
| **`builderExtendsModelRoot = false` (default)** | Builder and finished model stay distinct types. | Clean separation between mutable `Builder` and immutable built model — simplest to reason about. | Always requires an explicit `._build()` before passing the value anywhere typed to the model root interface. |

Reference: [`Krystal-models.md` § builderExtendsModelRoot](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/Krystal-models.md#builderextendsmodelroot);
`references/examples.md`'s `InnerDataV2`/`RestGetMappingLatticeSample` example.

## 7. Will this model be consumed across module/project boundaries (`isShared`)?

**Ask:** "Will other modules or repositories generate their own code on top of this specific model root — i.e.
is this genuinely a shared/platform model — or is it private to this module?"

| Option | Meaning | Pros | Cons |
|---|---|---|---|
| `isShared = true` | Marks the model as intended for cross-module/cross-project use. | Signals intent explicitly in the type declaration itself; generated code that other modules build on is placed in a `.gen` subpackage specifically to avoid split-package issues across module boundaries. | Different generated package layout that downstream consumers need to know about — not something to flip casually once other modules already depend on the un-shared layout. |
| **`isShared = false` (default)** | Ordinary, module-local model. | Simplest — generated code lives alongside the model root, no special subpackage logic to explain. | If the model turns out to need cross-module sharing later, introducing the package-layout change may not be transparent to existing consumers. |

Reference: [`ModelRoot.java`](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/src/main/java/com/flipkart/krystal/model/ModelRoot.java)'s
`isShared` doc comment; [`Krystal-models.md` § `@ModelRoot` Annotation](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/Krystal-models.md#modelroot-annotation)
table (`isShared` row).

## 8. What's the optionality/strictness philosophy for fields (`@IfAbsent`)?

This is decided **per field**, not once for the whole model — but ask about the overall stance up front (then
confirm/override per field as you write them), since it's easy to default every field to whatever the model's
`type` implies without thinking about it:

**Ask, per field:** "If a caller/serializer doesn't supply a value for this field, should that be impossible
(`FAIL`), tolerated with app-level handling (`WILL_NEVER_FAIL`), tolerated with an automatic zero-like default
(`ASSUME_DEFAULT_VALUE`), or mandatory only under specific conditions you can name (`MAY_FAIL_CONDITIONALLY`,
REQUEST-side only)?"

| Option | Meaning | Pros | Cons |
|---|---|---|---|
| `FAIL` | Missing value throws `MandatoryFieldMissingException`. | Strongest guarantee for consumers — a `RESPONSE` field with `FAIL` is a contract that clients can rely on always being populated. | Any legitimate "sometimes absent" case must be caught before it reaches this model, or building fails at runtime — wrong choice for genuinely optional data. |
| `WILL_NEVER_FAIL` | Optional; code handles absence gracefully. Field type must be `@Nullable T`/`Optional<T>`. | Matches truly optional data honestly — the type signature (`Optional`/`@Nullable`) forces every consumer to handle the absent case. | Slightly more ceremony at every call site (`Optional`/null-checks) than a plain mandatory field. |
| `ASSUME_DEFAULT_VALUE` | Absent ⇒ a type-specific zero-like default (0, `""`, empty collection, `false`, empty model, or an enum's `@DefaultValue` constant). | Enables real wire-size optimization in protocols like Protobuf (defaults aren't transmitted); no `Optional`/null-handling needed at call sites. | The "default" and "genuinely was zero" cases become indistinguishable to the consumer — wrong choice if that distinction matters. |
| `MAY_FAIL_CONDITIONALLY` | Conditionally mandatory; needs `conditionalFailureInfo` documenting when. REQUEST-side only (dual-type models allowed; **compile error in RESPONSE-only models**). | Documents "usually present, but here's exactly when it might not be" more expressively than a plain optional. | Easy to write a vague `conditionalFailureInfo` that doesn't actually help a future reader — only worth it if you can state the condition precisely. |

**This choice is a one-way door once the model has real callers** — flipping a field between mandatory and
optional later is a breaking change to every consumer, not a quick tweak. Decide deliberately now.

Reference: [`Krystal-models.md` § `@IfAbsent`](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/Krystal-models.md#ifabsent);
[`IfAbsent.java`](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/src/main/java/com/flipkart/krystal/model/IfAbsent.java).

## 9. (Enum model roots only) Which constant absorbs unknown/missing values?

**Ask:** "This is an enum model — confirm `UNKNOWN` is the first constant, and mark it (or whichever constant
should be the fallback) with `@DefaultValue`. Is `UNKNOWN` really the right fallback semantics here, or does
this enum need a different 'nothing decided yet' constant?"

This isn't really optional — `UNKNOWN`-first is enforced at compile time, and at least one constant needs
`@DefaultValue` for `@IfAbsent(ASSUME_DEFAULT_VALUE)` to be usable on a field of this enum type. It also
determines what a client sees when a newer server-side enum value is deserialized by older client code (falls
back to `UNKNOWN`/the JSON `@DefaultValue` constant rather than throwing) — worth confirming that's the
intended forward-compatibility behavior for this specific enum.

Reference: [`Krystal-models.md` § Enum ModelRoots](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/Krystal-models.md#enum-modelroots);
[`EnumModel.java`](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/src/main/java/com/flipkart/krystal/model/EnumModel.java),
[`DefaultValue.java`](https://github.com/flipkart-incubator/Krystal/blob/main/krystal-common/src/main/java/com/flipkart/krystal/model/DefaultValue.java).
