# Introduction
* Lattice is proposed webservice framework inside the Krystal ecosystem.
* Lattice allows developers to make their vajrams available as service APIs which can be invoked remotely with minimal additional code.

# Features
* Instantly turn a vajram into a service API with a single annotation.
* Pluggable support for standard wire protocols like json and protobuf
* All wire protocol schemas are auto-generated from Vajram definitions
* Deep integration with the krystal framework
* Easy definition and configuration of service objects via a simple fluent syntax.
* Integration with a central registry of services and vajrams.
* Out of the box integration with
  * resilience4j (via logic decorators)
  * tracing (OpenTracing compatible)
  * debugging with a standardized debugging header - both systemic and visual (with standardized "debug" header)
* Ability to plug standardization policies which unify common behaviours (like headers, tooling, configs etc.) across teams.

# Sample code
This following is a proposal and is just indicative - to give an idea of how things work. It is subject to change in the future.
## Service spec building
```java
public final class MyUsefulService implements LatticeServiceApplication<MyUsefulApplicationContext> {
  public LatticeService<MyUsefulApplicationContext> buildServiceSpec(){
    return LatticeService.builder()
            .serviceName("myusefulservice") // Globally unique service name - all apis in this service have the following format
                                            // <protocol><hostname>/<servicename>/<vajramId>
            .doc("My useful service makes my useful vajrams usefully available to all my awesome clients!")
            .withVajramGraph(buildVajramGraph()) // The DAG containing all the registered vajrams.
                                                 // All Remote Vajrams are auto registered as individual APIs
                                                 // The api names are same as the vajramIds.
                                                 // Batching is enabled by default for all of these APIs - this is invisible to the vajrams
            .networkProtocol(HTTP_2) // or HTTP_1_1, HTTP_3, TCP, FTP etc...
            .serviceProtocol(GRPC) // or REST, GRAPHQL etc...
            .defaultWireProtocol(PROTOBUF_3) // or JSON, PROTOBUF_2, THRIFT etc... Individual vajrams may override this if they choose.
            .serviceOwner("my-awesome-team@flipkart.com")
            .build();
  }
 
  private VajramKryonGraph buildVajramGraph(){
    VajramKryonGraph graph = VajramKryonGraph
            // Load all vajrams from java classes with these package prefixes.
            // These vajrams include `RemoteVajram`s which are external facing, as they are bound to remotely callable APIs (may be `ComputeVajram`s or `IOVajrams`),
            // and internal `ComputeVajram`s or `IOVajram`s which are direct/indirect dependencies of the above `RemoteVajram`s.
            .loadFromClasspath("com.flipkart.myusefulvajrams", "com.flipkart.myotherusefulvajrams").build();
    // `putUsefulInfo` is an IO vajram with maxBatchSize 20
    graph.registerInputBatchers(vajramID("putUsefulInfo", InputBatcherConfig.simple(() -> new Batcher<>(20))));
    // `getUsefulInfo` is an IO vajram with maxBatchSize 100
    graph.registerInputBatchers(vajramID("getUsefulInfo", InputBatcherConfig.shared(() -> new Batcher<>(100)))); 
    return graph;
  } 
}
```
## Remote Vajram
A `RemoteVajram` is a vajram which can be invoked remotely from another process over the network

```java
@RemotelyInvocable // This tells Lattice that this vajram can be bound to a service API.
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

  @Resolve(dep = "usefulData", depInputs = "queryInputs")
  static UsefulDataQuery computeQuery(@Using("userId") String userId, @Using("dataCount") Optional<Integer> dataCount){
    return UsefulDataQuery.builder()
            .userId(userId.trim())
            .infoCount(dataCount.orElse(DEFAULT_DATA_COUNT))
            .build()
  }

  @Output
  static MyResponse computeResponse(@Using("usefulData") UsefulDataDAO usefulData){
    return transform(usefulData);
  }
  
}
```
