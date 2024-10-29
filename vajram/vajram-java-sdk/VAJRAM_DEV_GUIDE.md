# How to write a vajram
## How to read this document
* This is a normative document intended to guide developers when coding vajrams.
* There is a companion video with live coding examples which walks through the document.
* This document follows [RFC 2119](https://datatracker.ietf.org/doc/html/rfc2119) in the use of prescriptive keywords. 
* [iff](https://en.wikipedia.org/wiki/If_and_only_if) is used for bidirectional implication. 
## Basics
Vajrams are units of business logic which are  **concurrently executable** and **composable** 
Let us try and understand each of these individually
* **Concurrently executable**: A vajram might need perform multiple related sub-tasks to achieve its functionality. The Krystal platform understands that some of these tasks could be executed independently while some might be dependent on the completion of other sub-tasks. To achieve the best performance, the paltform executes these sub-tasks in a highly concurrent fashion - meaning every sub-task is executed as soon as all the inputs that it needs are ready.
* **Composable**: A vajram is composable by design. This means that another vajram can add a dependency on this vajram and use its functionality as a sub-task to complete a larger, higher-order task. All these vajrams, and their dependencies form a DAG (Directed Acyclic Graph). The "concurrently executable" feature applies to the whole DAG as well as every individual vajram

## When is it appropriate to write a vajram?
This means the ideal use of a vajram is to compose business logic which involves making network calls to one or more remote APIs. Expecially, when a peice of logic needs to be reused to compose other business logic which depend on this.
Let us take an example: Let's say we need write code to email a user, given the user id, subject, and body. To do this, we need to first call the `UserService` to retrieve the user's `emailId` (Let's assume that this particular task is already present in a `GetEmailId` vajram which accepts a `userId` and returns their `emailId`) and then call a `MessengerService` to send the email (let's assume that this particular task is already present in a `SendEmail` vajram which accepts an `emailId`, `subject` and `body`). This is a great use case to write a vajram - `SendEmailToUser` which accepts `userId`, a subject and a body.  This new vajram is **composed** of the pre-existing two vajrams and stitches a higher order call graph which performs some functionality. To take the example further, let's say we have a new use case in which we need to place an order for a user and then send them a confirmation email. Again, this is a perfect usecase for writing another vajram `CompleteOrder` which is depends on (or is **comsposed** of) `CreateOrderEntry` vajram and `SendEmailToUser` vajram. We can see how the `SendEmailToUser` call graph containing 2 API calls is reused inside a bigger callgraph containing 3 API calls.

On the other hand let us say you have a piece of business logic which does not make any IO calls (neither network nor disk) either directly or indirectly - it just accepts some inputs, processes data, and returns some value -  this functionality need not need to be written as a vajram - it can just be written as a static method which can be called directly. This avoids the performance penality of modelling this logic as a node in a DAG.

## Steps 
1. Define the **Scope**: First, decide on the functional scope of the Vajram. What is the core functionality of the vajram, or what does it do? 
   1. If this functionality directly or indirectly involves making an IO call, then write a vajram. Else, write it as a plain method which can be called directly.
   2. Decide on the data type of the vajram's reponse. Response types must be immutable to avoid data races. (More on the reasons for this a bit later).
2. **Name** the vajram: Based on the above, decide on a name based on the [Vajram naming conventions](VAJRAM_NAMING_GUIDE.md).
3. **Create Vajram Class**
   1. Each Vajram is defined an **abstract** java class in the [Vajram](src/main/java/com/flipkart/krystal/vajram/Vajram.java) class hierarchy with the [`@VajramDef`](src/main/java/com/flipkart/krystal/vajram/VajramDef.java) annotaion placed on it. 
   2. The simple name of the java class should be same as the vajram name.
   3. The vajram name is a globally unique id which is, by default, inferred from the simple name of the java class (package name is ignored). The `@VajramDef` annotation accepts the vajram name as an optional string parameter in case the vajramName is not same as the java class name.
   4. If the vajram **directly** makes an IO call, the class must extend the [IOVajram](src/main/java/com/flipkart/krystal/vajram/IOVajram.java) class. Else it must extend the [ComputeVajram](src/main/java/com/flipkart/krystal/vajram/ComputeVajram.java) class.
4. **Finalize input contracts**: What are the bare minimum set of inputs needed by this vajram to perform its task? The inputs names and data types and optionality form the client-facing contract of the vajram. Inputs are written in the following format
 Steps to create an input:
   1. Inputs should be minimal: Decide the minimal sets of inputs needed to specify the parameters for the functionality of a vajram. Keeping this input set minimal is important because inputs are used by the plaform to enforce idempotency via caching. The hashcode of inputs is computed to do this. Having too many inputs or very large inputs can negatively impact performance.
   2. Choose **Input name**: What are names of the inputs. Input names:
      1. should be lowerCamelCase
      2. should be descriptive
      3. must be unique within the vajram
      4. must be unchanging across releases if you want to avoid breaking your clients.
   3. Decide **Input Data Type**: the data types of these inputs must be **deeply immutable** to avoid data races - this is important since the execution is concurrent. The order of execution of various vajrams might vary for every execution based on IO latencies and availability. This means data races can lead to non-determinacy and thus unpredictable logicExecResults.
   4. Decide **Input sources**: What is the right source of each input? Should it be provided by the client? or the runtime environment? or both?
      1. `@Input`: These are inputs which can only be provided by the clients of this vajram
      2. `@jakarta.inject.Inject`: These are inputs which can only be provided by the runtime via injection(simlar to the [dependency injection](https://en.wikipedia.org/wiki/Dependency_injection) design pattern)
      3. `@Input @jakarta.inject.Inject`: There are inputs which can be provided either by clients. But if clients provide a `null` value, then the runtime provided value is used.
   5. Decide **Optionality**: Inputs may be mandatory or optional. Optional inputs are marked using the `java.util.Optional` wrapper. 
      1. In a new vajram, care must be taken when marking an input as Optional. Once an input is marked Optional, it can not be changed to mandatory without breaking clients. In a future version of the platform, Krystal might fail build if this is done.
      2. Similarly, in an existing vajram care must be taken when marking a new input as mandatory - since this will break clients. In a future version of Krystal, this may be disallowed by breaking build.
      3. Optionality guidelines: 
         1. When writing a new vajram, prefer marking inputs as mandatory. An input should be optional iff there is a good, sensible default which is generic enough that it is valid in most scenarios.
         2. When adding new inputs to an existing vajram which has clients, only Optional inputs should be added. The defaults for the new inputs should handle existings clients' traffic in a backward compatible way.
   6. Decide the **Input Batching** strategy: Input batching is the process of merging/batching multiple executions of a vajram with different inputs into a single vajram call. This is generally useful as a performance optimization - to do less work than would otherwise be needed, or to opimize IO calls by minimizing chattiness. You can read more on this [here](README.md#input-batching).
      1. Inputs which need batching should be marked via the [@Batch](src/main/java/com/flipkart/krystal/vajram/batching/Batch.java) annotation
      2. Batching is not part of the contract with clients so this can be added/changed as needed without breaking clients.
5. After adding all inputs to the vajram class, build the project to trigger model generation.
   1. This can be done by performing `./gradlew build` or `./gradlew codeGenVajramModels`.
   2. At this point the vajram code generator plugin with run annotation processors on all classes with the `@VajramDef` annotation to generate data classes.
6. Declare dependencies
7. Declare input resolvers
   1. Input resolvers are logic which are responsible for resolving the inputs of a vajram dependency. They can also be used to skip the computation of dependency vajrams based on some predicate
   2. There are 2 ways to declare input resolvers:
      1. **input resolver methods**: these are declared by annotating a method with the [`@Resolve`](src/main/java/com/flipkart/krystal/vajram/facets/resolution/sdk/Resolve.java) annotation. These methods are part of the vajram class itself. The method takes inputs of already computed facets and using them decllares how to compute a particular input of a dependency.
           1. Sample code For a vajram with field testField_s which has to be computed using dependency Vajram called DependencyVajram, resolving 1 of the inputs of DependencyVajram:
            ```java
            @Resolve( depName = testField_n, depInputs = DependencyRequest.fieldOne_n)
            public static SingleExecute<Integer> resolveFieldOneForTest(Errable<Integer> input) {
               if(true) {
                   return SingleExecute.skipExecution("skipping");
               }
               return SingleExecute.executeWith(input.valueOpt().get());
             }
            ```
            2. The method parameters of the resolver method are tied to facets based on naming convention, you can use any facet as input with the variable name matching the facet name.
            3. The type of parameter can be `Errable<T>`, `Optional<T>` or T , where T is the raw type of the facet. If that facet is mandatory you have to use the raw type. If the facet is optional you have to use `Optional<T>` or `Errable<T>`.
            4. The return type can be I, `Optional<I>`, `SingleExecute<I>`, `MultiExecute<I>`, `? extends Collection<I>` where I is the raw type of dependecy input we are computing.
               Or it can R or `? extends Collection<R>` where R is Dependency Vajrams request object
               5. Two ways to skip in a resolver - Either throwing exception or returning `SingleExecute.skipExecution("reason")`. In case we are throwing an exception,
                  it is important to note that too many exceptions can create performance issues due to creation of stack traces. For this reason krystal provides a custom Exception class [StackTracelessException](../../krystal-common/src/main/java/com/flipkart/krystal/except/StackTracelessException.java)
                  which can be thrown as is or can be extended to create custom exceptions
               6. I, R, `Optional<I>`, `SingleExecute<I>` hast to be used if the input is non fanout input
               7. MultiExecute<I>`, `? extends Collection<I>`, `? extends Collection<R>` has to be used if the input is fanout input // TODO add link to a separate doc with examples and further explanation
      2. **Simple Input Resolvers**: "getSimpleInputResolvers" is a method which is part of the vajram class. This method can be overriden and a DSL for declaring input resolvers can be used.
           1. Sample code For a vajram with field testField2_s which has to be computed using dependency on another Vajram called DependencyVajram:
            ```java
            resolve(
              dep(
                  testField2_s,
                   depInput(DependencyRequest.fieldOne_s).usingAsIs(DependencyTestRequest.fieldOne_s).asResolver(), //this is a single resolver for field DependencyRequest.fieldOne_s
                    depInput(DependencyRequest.fieldTwo_s).usingValueAsResolver(() -> 2)
              )
            )
            ```
          2. Constraints- You can use maximum 2 sources inputs to compute a single input for a dependency vajram
      3. Constraints -
         1. There should be exactly 1 single fanout resolver (//define fanout resolver) in a vajram class for a given fanout dependency.
         2. Resolvers should not make any IO calls
         3. You cannot create a cyclic loop using resolvers, otherwise the build will fail
8. Add a method with the @Output annotation.
   1. Every vajram must have exactly one method with the @Output annotation. This is the method responsible for computing the final output of the vajram, and is executed only after the dependencies have executed.
9. Build and generate vajram impl class.
