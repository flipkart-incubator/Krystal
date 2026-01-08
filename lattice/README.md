# Introduction

* Lattice is an application development framework (in its beta) inside the Krystal ecosystem.
    * While the vajram-java-sdk provides a programming model for defining business logic, lattice
      aims to provide the runtime container for packaging and executing the business logic.
* Lattice allows developers to make their vajrams available as service APIs which can be invoked
  remotely with minimal additional code.
    * Note: While lattice has been written to extend the krystal ecosystem, and hence works well
      with vajrams, this is not mandatory. Developers can use lattice even with other programming
      models.

# Features

* Instantly turn a vajram into a service API just using annotation.
* Pluggable support for standard wire protocols like json and protobuf (and more can be added
  independently)
* All wire protocol schemas are auto-generated from Vajram definitions and/or standard java
  interfaces
* Deep integration with the krystal framework
* Easy definition and configuration of service objects via a simple syntax
* Integration with a central registry of services and vajrams.
* Out of the box integration with
    * resilience4j (via logic decorators)
    * tracing (OpenTracing compatible)
    * debugging with a standardized debugging header - both systemic and visual (with standardized "
      debug" header)
* Ability to plug standardization policies which unify common behaviours (like headers, tooling,
  configs etc.) across teams.

# Key Concepts

If you are familiar with Krystal framework in general, you would already be aware of concepts like
Vajram (The smallest unit of computation in the Krystalline programming paradigm). Lattice
introduces the following new concepts:

* [**LatticeApplication**](
  lattice-core/src/main/java/com/flipkart/krystal/lattice/core/LatticeApplication.java): A
  lattice application is a packagable, executable container which wraps
  business logic and is the root entity.
* [**Dopant**](lattice-core/src/main/java/com/flipkart/krystal/lattice/core/doping/Dopant.java): A
  lattice application gains functionality when an application author adds "Dopant"s to
  the
  lattice application. A dopant is a pluggable component which extends the functionality of the
  lattice application. The act of adding a dopant to a lattice app is called "doping". A lattice
  application without any dopant is no-op which exits instantly on
  startup. The name dopant
  is [inspired from material sciences](https://en.wikipedia.org/wiki/Dopant). Some
  examples of
  dopants:
    * VajramDopant: Allows a lattice application to interact with a [
      `VajramGraph`](../vajram/vajram-krystex/src/main/java/com/flipkart/krystal/vajramexecutor/krystex/VajramGraph.java)
    * KrystexDopant: Allows a lattice application to execute business logic using a [
      `KrystexGraph`](../vajram/vajram-krystex/src/main/java/com/flipkart/krystal/vajramexecutor/krystex/KrystexGraph.java)
    * ThreadingStrategyDopant: Allows application owner to control how threads are created and
      managed inside the application
    * RestServiceDopant: Adds capability to the lattice application such that it can behave as a
      restful web service
* **DopantType** : Every dopant has a unique dopant type (which is a string)
* **DopantAnnotation**: A dopant can have a corresponding Dopant Annotation which can be used by the
  application author to apply the dopant to the application. Dopant Annotations are placed on
  application classes. Some of the behaviours of a dopant can be specified using this annotation.
  Using a dopant Annotation to specify dopant behaviour allows tooling like annotation processors to
  use this information at application build time to generate code or perform validations, for
  example.
* [**DopantSpec**
  ](lattice-core/src/main/java/com/flipkart/krystal/lattice/core/doping/DopantSpec.java):
  The specification for a dopant. Every dopant must have a corresponding dopantSpec type. A Lattice
  author defines the behaviour of a dopant using its
  DopantSpec. Dopant Specs allow for programmatically defining the behaviour of a dopant. This gives
  the app author more flexibility, but less build-time visibility.
* [**DopantSpecBuilder**
  ](lattice-core/src/main/java/com/flipkart/krystal/lattice/core/doping/DopantSpecBuilder.java):
  A builder for creating a dopantSpec. Application authors return an instance of this to the lattice
  framework which
  then uses it to create a dopantSpec at a later point in time once other dopants which depend on
  this dopant get a chance to further enhance/modify the dopant spec builder. This architecture is
  core to lattice as it allows
    * Dopants to depend on each other
    * Dependent dopants to configure the dopants they depend on
    * Organizations to apply standardization policies so that dopants are used in a standard way
* [**DopantConfig**
  ](lattice-core/src/main/java/com/flipkart/krystal/lattice/core/doping/DopantConfig.java): A dopant
  can choose to have a dedicated dopant config object. Dopant configs are used to configure the
  dopant behaviour from outside the applications
* **DependencyInjectionFramework** : Dependency injection frameworks are fundamental to modern
  application development and thus is a first class construct in lattice (i.e it is not a dopant).
  The framework type is defined in the `@LatticeApp` annotation and is used control the code
  generation and runtime handling of application managed objects.

# Sample code

## Application spec definition

```java
@LatticeApp(
    description = "My Useful Service",
    dependencyInjectionFramework = CdiFramework.class) // Guice is also supported

// This dopant annotation makes this application into a restful web-service. Lattice applications
// can be used to build web-services, cron jobs, as well as command-line terminals
@RestService( // grpc and GraphQl are also supported
    resourceVajrams = {
      // List the vajrams which need to treated as REST APIs
      // The serialization protocols of the request and response of each is controlled at the vajram
      // definition
      API_1.class,
      API_2.class
    })
public abstract class MyUsefulService extends LatticeApplication {
  // Specifies that this application should create a pool of threads and dedicate one thread to each
  // incoming request
  @DopeWith
  public static ThreadingStrategySpecBuilder threading() {
    return ThreadingStrategySpec.builder().threadingStrategy(POOLED_NATIVE_THREAD_PER_REQUEST);
  }

  @DopeWith
  public LatticeService<MyUsefulApplicationContext> buildServiceSpec() {
    return LatticeService.builder()
        .serviceName(
            "myusefulservice") // Globally unique service name - all apis in this service have the
        // following format
        // <protocol><hostname>/<servicename>/<vajramId>
        .doc(
            "My useful service makes my useful vajrams usefully available to all my awesome clients!")
        .withVajramGraph(buildVajramGraph()) // The DAG containing all the registered vajrams.
        // All Remote Vajrams are auto registered as individual APIs
        // The api names are same as the vajramIds.
        // Batching is enabled by default for all of these APIs - this is invisible to the vajrams
        .networkProtocol(HTTP_2) // or HTTP_1_1, HTTP_3, TCP, FTP etc...
        .serviceProtocol(GRPC) // or REST, GRAPHQL etc...
        .defaultWireProtocol(
            PROTOBUF_3) // or JSON, PROTOBUF_2, THRIFT etc... Individual vajrams may override this
        // if they choose.
        .serviceOwner("my-awesome-team@flipkart.com")
        .build();
  }

  private VajramKryonGraph buildVajramGraph() {
    VajramKryonGraph graph =
        VajramKryonGraph
            // Load all vajrams from java classes with these package prefixes.
            // These vajrams include `RemoteVajram`s which are external facing, as they are bound to
            // remotely callable APIs (maybe `ComputeVajram`s or `IOVajrams`),
            // and internal `ComputeVajram`s or `IOVajram`s which are direct/indirect dependencies
            // of the above `RemoteVajram`s.
            .loadFromClasspath("com.flipkart.myusefulvajrams", "com.flipkart.myotherusefulvajrams")
            .build();
    // `putUsefulInfo` is an IO vajramDef with maxBatchSize 20
    graph.registerInputBatchers(
        vajramID("putUsefulInfo", InputBatcherConfig.simple(() -> new Batcher<>(20))));
    // `getUsefulInfo` is an IO vajramDef with maxBatchSize 100
    graph.registerInputBatchers(
        vajramID("getUsefulInfo", InputBatcherConfig.shared(() -> new Batcher<>(100))));
    return graph;
  }
}

```

## Remote Vajram

A `RemoteVajram` is a vajramDef which can be invoked remotely from another process over the network

```java

@RemotelyInvocable // This tells Lattice that this vajramDef can be bound to a service API.
@VajramDef
abstract class GetUsefulData extends ComputeVajram<MyResponse> {

  private static final int DEFAULT_DATA_COUNT = 10;

  @Input(idx = 1) // Every facet must have a unique idx
  // Input indexes are used when needed for wire formats like protobuf
  // (These might also be used for optimal switch cases in auto-generated data classes as well)
  // Backward compatibility checkers will make sure idxes are unique and do not change for RemoteVajrams
  String userId;

  @Input(idx = 2)
  Optional<Integer> dataCount;

  @Dependency(idx = 3, value = "GetUsefulDataFromDB")
  UsefulDataDAO usefulData;

  @Resolve(depName = "usefulData", depInputs = "queryInputs")
  static UsefulDataQuery computeQuery(@Using("userId") String userId,
      @Using("dataCount") Optional<Integer> dataCount) {
    return UsefulDataQuery.builder()
        .userId(userId.trim())
        .infoCount(dataCount.orElse(DEFAULT_DATA_COUNT))
        .build()
  }

  @Output
  static MyResponse computeResponse(@Using("usefulData") UsefulDataDAO usefulData) {
    return transform(usefulData);
  }

}
```
