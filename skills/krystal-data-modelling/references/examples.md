# Worked `@ModelRoot` examples

Every snippet below is drawn verbatim (or near-verbatim, with minor trimming noted) from real, compiling sample
modules in [flipkart-incubator/Krystal](https://github.com/flipkart-incubator/Krystal) — a separate codebase
from wherever you're modelling data. Each heading links to the real file on GitHub so you can pull the full
context (imports, surrounding classes, tests) if these excerpts aren't enough.

## A REQUEST model — primitives, optionality, nested model

[`JsonRequest`](https://github.com/flipkart-incubator/Krystal/blob/main/lattice/samples/rest/json/quarkus/rest-json-quarkus-lattice-sample/src/main/java/com/flipkart/krystal/lattice/samples/rest/json/quarkus/sampleRestService/models/JsonRequest.java)
— every `@IfAbsent` value used on a single model, plus a `ByteArray` field and a nested model:

```java
@SupportedModelProtocol(Json.class)
@SupportedModelProtocol(PlainJavaObject.class)
@ModelRoot(type = REQUEST)
public interface JsonRequest extends Model {

  @Nullable Integer optionalInput();

  @IfAbsent(FAIL)
  int mandatoryInput();

  @IfAbsent(value = MAY_FAIL_CONDITIONALLY, conditionalFailureInfo = "In some scenarios")
  @Nullable Integer conditionallyMandatoryInput();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  int inputWithDefaultValue();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Integer> repeatedInts();

  @IfAbsent(FAIL)
  ByteArray defaultByteString();

  @Nullable InnerData innerData();
}
```

## A RESPONSE model — impure, collections of models/enums, `Map`

[`JsonResponse`](https://github.com/flipkart-incubator/Krystal/blob/main/lattice/samples/rest/json/quarkus/rest-json-quarkus-lattice-sample/src/main/java/com/flipkart/krystal/lattice/samples/rest/json/quarkus/sampleRestService/models/JsonResponse.java)
(trimmed) — note `pure = false` (this particular model isn't pure, so it can't add `Protobuf3`), a
`@DefaultSerdeProtocol`, and fields covering `List<Model>`, `Map<String, Model>`, and enum fields/lists:

```java
@ModelRoot(type = RESPONSE, pure = false)
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Json.class)
@DefaultSerdeProtocol(Json.class)
public interface JsonResponse extends Model {

  @IfAbsent(WILL_NEVER_FAIL)
  Optional<Integer> optionalInteger();

  int mandatoryInt();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, String> mapTypedField();

  @IfAbsent(WILL_NEVER_FAIL)
  InnerData nestedData();

  @IfAbsent(WILL_NEVER_FAIL)
  List<InnerData> nestedDataList();

  @IfAbsent(WILL_NEVER_FAIL)
  Map<String, InnerData> namedInnerData();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Priority priority();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Priority> priorities();
}
```

The dual-type nested model it references —
[`InnerData`](https://github.com/flipkart-incubator/Krystal/blob/main/lattice/samples/rest/json/quarkus/rest-json-quarkus-lattice-sample/src/main/java/com/flipkart/krystal/lattice/samples/rest/json/quarkus/sampleRestService/models/InnerData.java)
— is `type = {REQUEST, RESPONSE}`, which is exactly why both `JsonRequest` and `JsonResponse` above can each
nest it:

```java
@ModelRoot(type = {REQUEST, RESPONSE})
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Json.class)
public interface InnerData extends Model {
  @IfAbsent(FAIL)
  String value();

  @IfAbsent(FAIL)
  int count();
}
```

## `builderExtendsModelRoot` — a builder usable as the model itself

[`InnerDataV2`](https://github.com/flipkart-incubator/Krystal/blob/main/lattice/samples/rest/json/quarkus/rest-json-quarkus-lattice-sample/src/main/java/com/flipkart/krystal/lattice/samples/rest/json/quarkus/sampleRestService/models/InnerDataV2.java):

```java
@ModelRoot(
    type = {RESPONSE},
    builderExtendsModelRoot = true)
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Json.class)
public interface InnerDataV2 extends Model {
  String value();
  int count();
  List<InnerData> innerData();
}
```

This is what lets a Vajram accept an injected, partially-built response builder as one of its own facets and
pass it straight through — see
[`RestGetMappingLatticeSample`](https://github.com/flipkart-incubator/Krystal/blob/main/lattice/samples/rest/json/quarkus/rest-json-quarkus-lattice-sample/src/main/java/com/flipkart/krystal/lattice/samples/rest/json/quarkus/sampleRestService/logic/RestGetMappingLatticeSample.java),
which takes `JsonResponse_Immut.@Nullable Builder responseBuilder` as an `@Output` parameter and finishes
building it:

```java
@Output
static JsonResponse response(
    String fullPath, String name, String age, UriInfo uriInfo,
    JsonResponse_Immut.@Nullable Builder responseBuilder) {
  return requireNonNullElseGet(responseBuilder, JsonResponse_ImmutJson::_builder)
      .path(fullPath)
      .qp_name(name)
      .qp_age(age)
      ._build();
}
```

## An enum model root

[`Priority`](https://github.com/flipkart-incubator/Krystal/blob/main/lattice/samples/rest/json/quarkus/rest-json-quarkus-lattice-sample/src/main/java/com/flipkart/krystal/lattice/samples/rest/json/quarkus/sampleRestService/models/Priority.java)
— no `@SerialId` at all (falls back to declaration order), `@DefaultValue` on the first (`UNKNOWN`) constant:

```java
@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Json.class)
public enum Priority implements EnumModel {
  @DefaultValue
  UNKNOWN,
  LOW,
  MEDIUM,
  HIGH,
}
```

[`Status`](https://github.com/flipkart-incubator/Krystal/blob/main/lattice/samples/grpc-proto3-lattice-sample/src/main/java/com/flipkart/krystal/lattice/samples/grpc/proto3/sampleProtoService/Status.java)
— same shape, but with explicit `@SerialId` (all-or-none, `UNKNOWN = @SerialId(0)`) because it supports
`Protobuf3`, which needs a stable wire index:

```java
@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Protobuf3.class)
public enum Status implements EnumModel {
  @DefaultValue @SerialId(0) UNKNOWN,
  @SerialId(1) PENDING,
  @SerialId(2) IN_PROGRESS,
  @SerialId(3) COMPLETED,
  @SerialId(4) FAILED,
}
```

## A pure, Protobuf3-only model — `@SerialId` all-or-none, nested model + enum + lists of both

[`SubMessage`](https://github.com/flipkart-incubator/Krystal/blob/main/lattice/samples/grpc-proto3-lattice-sample/src/main/java/com/flipkart/krystal/lattice/samples/grpc/proto3/sampleProtoService/SubMessage.java)
(also `builderExtendsModelRoot = true`):

```java
@ModelRoot(
    type = {ModelType.RESPONSE},
    builderExtendsModelRoot = true)
@SupportedModelProtocol(Protobuf3.class)
public interface SubMessage extends Model {
  @SerialId(1)
  @IfAbsent(FAIL)
  int count();

  @SerialId(2)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable ProtoMessage protoMessage();

  @SerialId(3)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<ProtoMessage> protoMessages();

  @SerialId(4)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Status subStatus();

  @SerialId(5)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Status> subStatuses();
}
```

Note every field carries `@SerialId` here (unlike `Priority` above) — required because `Protobuf3` needs a
stable wire index per field, and the all-or-none rule means once one field has it, they all must.

## A Fory model (`ForyRequest`)

[`ForyRequest`](https://github.com/flipkart-incubator/Krystal/blob/main/lattice/samples/rest/fory/quarkus/rest-fory-quarkus-lattice-sample/src/main/java/com/flipkart/krystal/lattice/samples/rest/fory/quarkus/sampleForyService/models/ForyRequest.java):

```java
@SupportedModelProtocol(Fory.class)
@SupportedModelProtocol(PlainJavaObject.class)
@ModelRoot(type = REQUEST)
public interface ForyRequest extends Model {

  @Nullable Integer optionalInput();

  @IfAbsent(FAIL)
  int mandatoryInput();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Integer> repeatedInts();

  @Nullable ForyInnerData innerData();
}
```

## Constructing, serializing, and deserializing generated models

From
[`JsonResponseTest`](https://github.com/flipkart-incubator/Krystal/blob/main/lattice/samples/rest/json/quarkus/rest-json-quarkus-lattice-sample/src/test/java/com/flipkart/krystal/lattice/samples/rest/json/quarkus/sampleRestService/JsonResponseTest.java) —
building via `_builder()`, passing a nested model's builder directly into the parent's setter (no `._build()`
needed on the nested value), serializing, and round-tripping via the byte-array constructor:

```java
JsonResponse_ImmutJson immutJson =
    JsonResponse_ImmutJson._builder()
        .string("Hello")
        .mandatoryInt(5)
        .nestedData(InnerData_ImmutJson._builder().value("Hello").count(11)._build())
        .priority(Priority.HIGH)
        ._build();

byte[] serializedPayload = immutJson._serialize();
JsonResponse_ImmutJson deserialized = new JsonResponse_ImmutJson(serializedPayload);
assertThat(deserialized).isEqualTo(immutJson);
```

Passing a nested model's `Builder` directly (POJO variant), and `_newCopy()` for a deep copy:

```java
JsonResponse_ImmutPojo.Builder immutJsonBuilder =
    JsonResponse_ImmutPojo._builder()
        .string("Hello")
        .mandatoryInt(5)
        .nestedData(InnerData_ImmutPojo._builder().value("Hello").count(11)) // builder, not built value
        .priority(Priority.MEDIUM);

assertThat(immutJsonBuilder._build()).isEqualTo(immutJsonBuilder._newCopy()._build());
```

Unknown-enum-value JSON deserialization falling back to `UNKNOWN` (same test class):

```java
String modifiedJson = json.replace("\"HIGH\"", "\"NONEXISTENT\"");
JsonResponse_ImmutJson deserialized = new JsonResponse_ImmutJson(modifiedJson.getBytes(UTF_8));
assertThat(deserialized.priority()).isEqualTo(Priority.UNKNOWN);
```

Serializing a request and sending it over HTTP, from
[`QuarkusRestEndpointsE2eTest`](https://github.com/flipkart-incubator/Krystal/blob/main/lattice/samples/rest/json/quarkus/rest-json-quarkus-lattice-sample/src/test/java/com/flipkart/krystal/lattice/samples/rest/json/quarkus/sampleRestService/QuarkusRestEndpointsE2eTest.java):

```java
var body =
    JsonRequest_ImmutJson._builder()
        .mandatoryInput(7)
        .mandatoryLongInput(99L)
        .defaultByteString(SimpleByteArray.copyOf("\0".getBytes(StandardCharsets.UTF_8)))
        ._build();

httpClient.send(
    HttpRequest.newBuilder(uri)
        .POST(BodyPublishers.ofByteArray(body._serialize()))
        .build(),
    BodyHandlers.fromSubscriber(
        BodySubscribers.mapping(BodySubscribers.ofByteArray(), JsonResponse_ImmutJson::new),
        BodySubscriber::getBody));
```
