The Vajram Programming Model
============================

## Introduction

The Vajram Programming model provides a way to design and write synchronous scatter-gather code. Apart from the 
general goals of the Krystal Project, the vajram progamming model is designed keeping in mind the following goals:

1. Easy self-serve programming: The Vajram programming model is designed to allow for multiple developers from
   different disconnected teams to code, test and deploy business logic for which they are the domain expert in their own 
   CVS repo but deployed into a common runtime environment at the same time minimizing chances of introducing regressions 
   in functionality and performance of the applications in which the code is being deployed.
1. Cross-feature and Cross-application re-usability: The same piece of business logic can be reused for different
   requirements within an application and also across different application runtimes.

## Programming Model Design

### Vajram

The basic building block of a Krystal is a Vajram. Each vajram is responsible for one piece of well-defined, reusable
business logic (possibly encapsulating a network call to an external system). Each Vajram is identified by its
identifier (VajramID). This identifier is globally unique and can be used as-is by adding it as a dependency of another
Vajram. This means that the business logic encapsulated within a Vajram need not be local to an application.. It can
theoretically be used within any runtime which supports the Krystal programming model. For example, Krystex.

Every Vajram is defined by the following properties

* Output Type (Schema): This is the datatype of the output of a Vajram. Each vajram has a single output with a well
  defined schema.
  Example:

    * Vajram1 can have output :
      ```
      Entity($ID){
        name 
        childEntity {
          ID
        }
      }
      ```
    * Vajram2 can have output :
      ```
      Entity($ID){
        childEntity{
          owner
        }
      }
      ```
    * Vajram3 can have output :

      ```
      Entity($PID){
        type
        children [{
          type
        }]
      }
      ```
* List of inputs. Each input has the following properties

* Input Resolution (Enum: STATIC, RUNTIME): Statically resolved inputs are other Vajrams whose output is to be used as an input
  to this Vajram - these Vajrams are dependencies of this Vajram. Runtime wired inputs on the other hand are '
  unresolved' at compile-time - meaning they do not refer to any other Vajram. These will be wired dynamically at runtime
  by the DAG executor. For example, let's say there is a Vajram which takes in a greeting Id and account Id, 
  calls AccountService and then performs a transformation on the output of Account Service. In this case, the productId 
  and listing Id are RUNTIME inputs, since they need to be resolved to this Vajram dynamically based on the use case. 
  On the other hand the AccountService dependency is a STATIC input.
* Input Type (Schema): Each input has a well-defined datatype. For static inputs, the datatype of the input is implicit
  and is the same as the data type of the output of the dependency Vajram. For runtime inputs, the data type needs to be
  explicitly defined.
* Requirement Level (Enum: MANDATORY, OPTIONAL): If a mandatory input fails, then this Vajram fails. If an optional
  input fails this Vajram might still succeed.

* Input Resolvers: While the runtime dependencies of this Vajram are wired by its dependents at runtime, the
  responsibility of binding the RUNTIME dependencies of its STATIC dependencies lies with this Vajram. This is done by
  input binders.

* Example: Let us say a Vajram V1 has two RUNTIME inputs R1 and R2, and two STATIC inputs S1 and S2. And let us say S1
  has one RUNTIME input itself, and S2 has two RUNTIME inputs. Input binders of Vajram V1 are pieces of business logic
  which take as input some subset of the dependencies of V1 and output data which are then bound to/wired into the
  RUNTIME dependencies of a static dependency of V1. This means that every STATIC dependency of a Vajram which has at
  least one RUNTIME dependency needs a corresponding input binder defined by V1. The static dependency S2 of V1 has 2
  RUNTIME inputs, so the input binder defined by V1 corresponding to S2 outputs a list of tuples, where each tuple
  contains two values - one corresponding to each of the RUNTIME inputs of S1.

* Input Modulation Strategy (For BlockingVajrams)

  (Examples: LAZY, EAGER, BATCHED(batch_size), CUSTOM): Eager by default. Eager Vajrams are executed the first instant
  they are resolved as a dependency in the Krystal. Lazy Vajrams wait till the DAG has finished resolving all
  dependencies, and only then are executed. Batched Vajrams are executed when the batch\_size of inputs is reached.
  Custom
  modulation strategy is used when more complicated requirements exist to merge multiple tuples of inputs into a single
  call. For example, if a Vajram takes as an input a parameter call "level" with possibilities LOW, MEDIUM, HIGH and has
  a
  need to execute only the highest level among the requested levels, the Vajram can define a custom input modulation
  strategy which waits for the DAG to resolve all its dependencies and then squashes multiple tuples of inputs
  containing
  LOW, MEDIUM and HIGH levels into a single input corresponding to the highest level.

* ExecutionParameters:

    * ExecutionIsolationMode(Enum: THREAD, SEMAPHORE, NONE): Vajrams performing blocking operations are preferred to
      return
      a Promise/CompletableFuture, but if this is not possible, these Vajrams can specify the parameters like threadpool
      key
      and threadpool size to indicate to Krystal that the node needs to be executed in its own dedicated thread pool.
    * ExecutionConcurrency(Integer): How many parallel instances of a Vajram can execute within the runtime.
    * CircuitBreakingStrategy: Optionally enabled circuit breaking behaviour.

* SDLC Metadata:

    * Owner Team:
    * POC Email:
    * Oncall alias:

* Documentation: Describing in detail what this Vajram does, and how to use it - and how not to. This documentation is
  indexed and will facilitate the discovery of this Vajram across the company.
* Permissions: RESTRICTED/PRIVATE - Vajram owners can decide who can declare a dependency on the Vajram.
* Vajrams can be of two types: STANDALONE and GENERATORs.

* STANDALONE Vajrams work as functions which take inputs and outputs. Their output does not have any direct impact on
  the structure of the Krystal.
* GENERATOR Vajrams have the ability to modify the Krystal by adding more Vajrams into the Krystal. The Brahmastra
  Orchestration platform makes use of these Vajrams. Application developers developing widgets will normally not have to
  interact with GENERATOR Vajrams

### Programming Spec

```java
import com.flipkart.userservice.UserServiceVajram;

/**
 * Given a userId, this Vajram composes and returns a 'Hello!' greeting addressing the user by 
 * name (as declared by the user in their profile).
 */
@VajramDef(ID) //Unique Id of this Vajram
//SyncVajram means that this Vajram does not directly perform any blocking operations.
public class GreetingVajram extends SyncVajram {
    public static final String ID = "com.flipkart.greetingVajram";

    // Static declaration of all the inputs of this Vajram.
    // This includes inputs provided by clients of this vajram, 
    // design-time dependencies of this vajram, as well as
    // objects like loggers and metrics collectors injected by the runtime.
    @Override
    public List<VajramDependencyDefinition> getDependencyDefinitions() {
        return Arrays.asList(
                UnresolvedInput.builder()
                        // Local name for this input
                        .name("user_id")
                        // Data type - used for code generation
                        .type(string())
                        // If this input is not provided by the client, throw a build time error.
                        .isMandatory()
                        .build(),
                ResolvedDep.builder()
                        // Data type of resolved dependencies is inferred from the 
                        // dependency Vajram's Definition
                        .name("user_info_fetch")
                        // GreetingVajram needs UserService's Response to compose the Greeting
                        // which it can get from the UserServiceVajram 
                        // (which is an Async Vajram as it makes network calls.
                        .dataAccessSpec(vajramID(UserServiceVajram.ID))
                        // If this dependency fails, fail this Vajram
                        .isMandatory()
                        .build(),
                UnresolvedInput.builder()
                        .name("log")
                        .type(java(Logger.class))
                        // This is not expected from clients
                        // This is expected to be injected by the runtime
                        .resolvableBy(ResolutionSources.SESSION)
                        .build(),
                SideEffectDep.builder()
                        // Data type of resolved dependencies is inferred from the 
                        // dependency Vajram's Definition
                        .name("push_analytics_event")
                        .dataAccessSpec(vajramID(AnalyticsEventSink.ID))
                        .build());
    }

    // Resolving (or providing) unresolved inputs of resolved dependencies
    // is the responsibility of 
    // this Vajram (unresolved inputs are resolved by client Vajrams).
    // In this case the UserServiceVajram needs a user Id to retrieve user info from User Service.
    // So it's GreetingVajram's responsibility to provide that input.
    @Resolve(value = "user_info_fetch", inputs = UserServiceVajram.USER_ID)
    public String userIdForUserService(@BindFrom("user_id") String userId) {
        return userId;
    }

    // This is the core business logic of this Vajram
    // Sync vajrams can return any object. AsyncVajrams need to return {CompletableFuture}s
    @VajramLogic
    public String call(String userId, UserInfo userInfo, Logger log, FdpEventSink fdpEventSink) {
        String greeting = "Hello " + userInfo.name() + "! Hope you are doing well!";
        log.info("Greeting user " + userId);
        fdpEventSink.pushEvent("event_type", new GreetingEvent(userId, greeting));
        return greeting;
    }

    @Resolve(value = "push_analytics_event")
    public FdpEvent pushAnalyticsEvent(
            @BindFrom("user_id") String userId,
            @BindFrom("user_info_fetch") UserInfo userInfo,
            //Side Effect dependency resolvers have access to output of this vajram
            String greeting) {
        return new AnalyticsEvent("UserGreetingEvent", new GreetingEvent(userId, greeting));
    }
}
```

## Design choices in Detail

### Static dependency declaration

One of the core tenets of Krystal is to allow for a reactive execution environment to minimize usage of resources like
threads. To achieve this Krystal

### Input Modulation


#### Definition

Input modulation is the process of squashing/collapsing/merging multiple independent inputs to a blocking operation so
that the blocking operation can compute outputs to these inputs in an optimal fashion consuming minimum resources like
network bandwidth, disk IO etc. Input modulation is a generalization of the concept we know as Batching.

##### Batching vs Modulation

Why are we calling this feature Input Modulation and not batching? This is because there might be other kinds of request
merging which are different from batching. Batching is a process where we take N requests objects each consisting of one
input parameter and merge them into a single request object containing a list of all N parameters. On the other hand,
let us say there is an API which provides three info levels within a given API - LOW, MEDIUM, HIGH. Depending on the
info level passed in the API, the server returns varying amounts of data — where LOW is a subset of MEDIUM which itself
is a subset of HIGH. If two clients request data from the API, one with the LOW info level and one with MEDIUM, we would
ideally want to make a single call with MEDIUM as the info level. Here the input modulation modulates both the requests
into a single request containing the MEDIUM info level.

#### Problems

Let us understand the philosophy and design Krystal Programming model's input modulation feature by considering a
hypothetical example. Let us consider a user path system which performs scatter gather operations to serve widgets to
the client side application. It has dependencies on multiple systems like Pricing Service, Entity Service (Entity
Service) etc. This orchestrator serves two widgets - product details and product Card on the same web page:

```
Entity Details

Manufacture Date: XX/YY/ZZZZ

Brand: ABC
```

```
Entity Card

Title: ABC Phone

Price: ₹12300.00
```

Note that the Title, manufacturer Date, and Brand are all served by the same API of Entity Service:

#### Batch API

```java
class EntityServiceClient {
    Map<String, String> getEntityAttributes(String productId, List<String> attributeNames) {
        return httpClient.get(EntityServiceUrl + "/" + productId + "/" + attributes.join(","))
    }
}
```

Since this Entity Service server is capable of returning multiple attributes of a given product in a single network
call, the client library API obviously reflects that capability.

##### Problem 1 - Leaking of implementation details to clients

This API is exposed to clients of Entity Service like Entity Details widget and Entity Card widget. This is not ideal.
Ideally the API contract that Entity Service would like to provide to its client looks like this:

#### Mono API

```java
class EntityServiceClient {
    String getEntityAttribute(String productId, String attributeName);
}
```

This interface hides the implementation detail that Entity Service API supports batching of attributes. A developer
writing a piece of business logic which needs data from Entity Service doesn't need to unnecessarily wrap the attribute
name in a list just to conform to the API, and then doesn't need to unwrap the response map to get the attribute value.
This makes the code simpler and representative of the actual requirements rather than the APIs internal details.

##### Problem 2 - Suboptimal batching

Now, to avoid problem 1, the Entity Service API library can provide both the batch API and the mono API, and clients can
choose to use the one that suits them. This way, if there is a client who needs multiple attributes of the same product,
they can send the list of attribute names in a list to the batch API and receive all the responses in a single call. But
this is still not optimal. What if there are two use cases in the application, both of which need different attributes
but for the same product? How can we make sure that both these dependencies can get the data that they need in a Entity
Service call? Blindly calling the batched API from the two call sites leads to two network calls. To avoid this,
application owners resort sub-optimal coding patterns where all dependencies of Entity Service are made to coordinate by
submitting their response to a common collector and once all are done submitting their requests and acknowledging the
same, the Entity Service batch call is made. This couples multiple different clients of Entity Service with each other,
and any time a new piece of business logic is written that needs Entity Service data, the new developer has to be aware
of the above coordination and has to register for the same. Any such miss can cause a significant increase in Entity
Service calls and reduction in system performance.

#### Design

To solve the above problems, the Krystal Programming model introduces the concept called input modulation. Every Vajram
which encapsulates potentially blocking logic, and can optimize its performance by merging/squashing multiple
independent requests into a single request, can declare its input Modulator.

An input modulator is a data structure that presents these APIs:

```java
interface InputModulator {
    List<ModulatedRequest> add(Request request);

    List<ModulatedRequest> modulate();

    void register(Consumer<List<ModulatedRequest>> listener);
}
```

##### add(...)

Whenever a client of Entity Service requests some data from Entity Service, the Krystal Runtime passes on this request
object to the add method of the input modulator of Entity Service vajram which stores it in memory and in most cases
returns an empty list. This is repeated for every other Vajram which depends on Entity Service. Every time a new request
is added to the input modulator, it checks if the current set of requests has reached the max batch size (a
configuration parameter of the input modulator). If this is the case, the input modulator squashes all the pending
requests into an optimal set of batched request objects and returns this list instead of an empty list. This is an
indication to the Krystal Runtime to trigger the actual Entity Service call now.

Sometimes some APIs do not specify a max batch size. This means the API is designed to support at most one call per
session irrespective of the number of requests. Also, even if a max batch size is defined, it is possible that all the
clients of the API have not requested enough data to reach the max batch size. In these scenarios, the add method never
returns a list of modulated requests and thus we need a way to proactively trigger the API call with the available set
of requests. This is where the modulate and register methods come in.

##### modulate(...)

Since the Krystal Programming model is based on Vajrams which statically declare their dependencies and because the
wiring and interaction between these Vajrams is completely under the control of the Krystal Runtime which wires these
dependencies together, the runtime has the necessary information to know when client Vajrams of Entity Service have
completed requesting data from Entity Service. There are multiple ways the runtime can do this we won't go into here (
See Krystex LLD for reference implementation). When the runtime realizes that the clients have finished requesting data
from Entity Service, the runtime calls the modulate method of the input modulator which merges all the pending requests
and returns the list of modulated requests which are then used by the Runtime to make the network call(s).

##### register(...)

Another strategy for modulation is for the input modulator to have a timeout for which it will wait for new requests.
After that timeout is breached, the input modulator modulates all the pending requests. When it does this, it needs a
way to notify the runtime that the modulation is done. So the runtime can register a listener via the register method.
This listener is called with the modulated input when the input modulator decides it is time to modulate. This listener
can then trigger the actual netwerk call.

![](assets/modulation_workflow.png)

#### Demodulation

If a client has requested data for attribute 'a', and the input modulator has modulated the input into a list of
attributes 'a', 'b' and 'c' and the Entity Service API returns the response as a Map contains the three attribute names
as keys and attribute values as values, it doesn't make sense to return this map to all the clients who then expected to
query the map for their data. We would want to keep the contracts clean and return only the data requested by a
particular client. This process of taking the response of a batched/modulated API and extracting out the necessary
information for a given unmodulated request is called demodulation and can be performed either by the input modulator,
or by the Blocking Vajram itself.

#### Rationale

The above design solves multiple problems

1. Decouples clients from server's implementation details by making batching completely invisible to clients - If
   tomorrow a new API with the same functional semantics, but a different batching strategy is written, we can just
   change the input modulator for the Vajram and keep the client contracts and code untouched.
2. Decouples clients from each other - Not keeping clients of your API independent can have significant negative
   consequences for evolvability and maintainability of their code. If the programming model doesn't hide details of
   batching from clients, then the clients will have to coordinate among themselves to collate their requests and only
   then trigger the API. This is bad because:
    1. clients now need to be aware of the dispatching order of their dependencies which is completely orthogonal to
       their business requirements.
    2. This increases the mental overhead of developers who will need to know the complete call graph of the application
       if they need to optimally code their business logic into an application.
    3. If we want to achieve cross application reusability of code, at development time - while integrating with an API,
       the developer cannot know the what other clients of the API are present in the runtime classpath, because the
       runtime is classpath is built at a later point in time and can change from application to application. This makes
       it even harder to coordinate among clients and will need additional abstractions to allow for this kind of
       coordination.
    4. In worst case scenarios, developers are forced to change their call graph structure itself, just to achieve
       optimal batching.
3. The tradeoff between Functional separation and optimal performance is no longer something to worry about. Clients can
   remain functionally separate and still the application can achieve optimal performance.
4. Avoid unnecessary spaghetti code: Clients of API which implement batching do not need to write the spaghetti code of
   modulating requests and demodulating responses. All this logic is encapsulated by the dependency Vajram
5. The functional contract accurately represents the domain of the server giving longevity to contracts.

#### Comparison with other technologies

##### [graphql/dataloader](https://github.com/graphql/dataloader)

The Graphql Dataloader solves the batching problem (not the general input modulation problem) for the javascript
programming language. The trigger for modulating is one tick in the javascript runtime eventloop, though users can
implement more sophisticated mechanisms where the wait time is longer than the eventloop tick. Since the java runtime
doesn't have an event loop, this cannot be achieved by a simple library. It needs a programming model which can enable
this - and the Krystal programming model does so. While the Dataloader solves for batching all requests within a tick of
the js eventloop, anything more sophisticated will need significant development from the application developer to
coordinate multiple callees.

The Krystal Programming model achieves the optimal batching across ticks of the eventloop because, apart from being a
framework running on the reactive model, it is also works on the principle of statically declared, granular dependencies
allowing it to optimally batch and modulate requests without any additional effort from the application developer.

## Comparison with other technologies
