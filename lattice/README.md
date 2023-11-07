# Introduction
* Lattice is proposed webservice framework inside the Krystal ecosystem.
* Lattice allows developers to make their vajrams available as service APIs which can be invoked remotely with minimal additional code.

# Features
* Instantly turn a vajram into a service API with a single annotation.
* Pluggable support for standard wire protocols like json and protobuf
* All wire protocol schemas are auto-generated from Vajram definitions
* Deep integration with the krystal framework
* Easy definition and configuration of service objects via a simple fluent syntax.
* Integration with a central repository of services and vajrams where vajrams are bound to services by the service descriptor

# Sample code
## Service spec building
```
public final class MyUsefulService implements LatticeServiceApplication<MyUsefulApplicationContext> {
  public LatticeService<MyUsefulApplicationContext> buildServiceSpec(){
    return LatticeService.builder()
            .serviceName("myusefulservice") // Globally unique service name - all apis in this service have the following format
                                            // <protocol><hostname>/<servicename>/<vajramId>
            .doc("My useful service makes my useful vajrams usefully available to all my awesome clients!")
            .withVajramGraph(buildVajramGraph()) // The DAG containing all the registered vajrams.
                                                 // All Remote Vajrams are auto registered as individual APIs - the api names are same as the vajramIds.
                                                 // Batching is enabled by default for all of these APIs - this is invisible to the vajrams
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
    graph.registerInputModulators(vajramID("putUsefulInfo", InputModulatorConfig.simple(() -> new Batcher<>(20))));
    // `getUsefulInfo` is an IO vajram with maxBatchSize 100
    graph.registerInputModulators(vajramID("getUsefulInfo", InputModulatorConfig.shared(() -> new Batcher<>(100)))); 
    return graph;
  } 
}
```
