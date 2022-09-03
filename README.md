# The Krystal Project

///// This doc is a WIP

Super-charging the development of synchronous and asynchronous business workflows.

## Introduction

The Krystal Project facilitates a developer-friendly way to write high-performant, complex, business logic which is easily maintainable and observabe. The project encompasses the following major components. 
   * Vajram: A programming model which allows developers to design and write code for synchronous scatter gather business logic in a 'bottom-up' (choreographed) manner.
   * Caramel: A programming model which allows developers to design and write code for synchronous and asynchronous workflows in a 'top-down' (orchestrated) manner.
   * Krystex: A runtime environment which executes synchronous parts of the code written in vajram/caramel programming models in an optimal way by understanding static dependencies between pieces of code, creating a logical Directed-Acyclic-Graph, and executing the DAG with maximal concurrency.
   * Raven: An asynchronous workflow orchestrator which orchestrates the asynchronous parts of worflows written in the caramel programming model.
   
## Bottom-up vs. Top-down programming
The Krystal project supports two ways of writing business logic - Bottom-up and top-down. 

### Bottom-up programming or Orchestration
Bottom-up programming is a paradigm in which a developer focuses on one atomic piece of business logic and it's dependencies without being aware of when, how and in what order the code will be executed with respect to other business logic in the application. The orchestration of the execution of the code is the reponsibility of the platform runtime, which analyzes the static dependencies declared by each piece of business logic and orchestrates the execution of the all the different pieces of the business logic in the most optimal fashion by building a Directed Acyclic Graph. This also called Orchestration.

### Top-down programming or Choreography
Top-down programming is a paradigm in which the developer wants complete control over when and in what order a piece of business logic is executed. This means that the developer has an awareness of the uber workflow and a design intent for ordering of the various operations which are part of the workflow in a specific pre-determined order.

### Example 1
Let's say a developer wants to write a piece of business logic that returns the population of the capital of a country, and let us say there are two exisinting APIs which return the capital of a given country and the pupulation of a given city respectively.

Bottom-up programming
```
//Pseudo-code
name: populationOfCapital
inputs: country
dependencies: 
  city = capital(country)
  population = population(city)
return:
  population
```

Top-down programming
```
inputs: country
workflow:
  return 
    getCapitalOfCountry(country)
    .getPopulationOfCity()
    .returnValue()
```

### Example 2
Let's say a developer is coding the recipe for making a pizza

Bottom-up programming
```
//Pseudo-code
----------
name: dough
owner: dev1 (kneader)
inputs: water, flour
dependencies: []
return:
  rest(water + flour, 5hours)
----------
name: pizzabase
owner: dev2 (base_maker)
inputs: water, flour, thickness, radius
dependencies: 
  dough = dough(water, flour)
return 
  shape(dough, thickness, radius)
----------
name: preheated_oven
owner: dev3 (oven_manager)
inputs: temp, time
dependencies: []
return
  preheat_oven(temp, time)
----------
name: standard_wheat_pizza
owner : dev4 (storefront)
inputs: water, wheat_flour, cheese, toppings[]
dependencies:
  base = pizzabase(water, wheat_flour, thickness = 5mm, radius = 9inch)
  oven = preheated_oven(200degCel, 15min)
return
  bake(oven, base + cheese + toppings, 20min, 200degCel)
  
```
As you can see, each piece of business logic is owned by a separate owner. And no single owner knows the complete end to end workflow. With each node declaring their local dependencies and interacting with those dependencies, the final pizza get's made without any single developer knowing the complete recipe. They just need to focus on their part of the problem statement (recipe).


Topdown programming
```
Pseudo code
---------
Owner: Dev5 (chef)
return:
       startWorkflow("Standard_wheat_pizza")
          .preheatOven(200degCel, 15min)
          .as(preheatedOven)
          .take(water, wheatFlour)
          .makeDough(water, flour)
          .as(dough)
          .makeBase(dough, 5mm, 9inch)
          .as(base)
          .add(cheese).on(base)
          .add(toppings).on(cheese)
          .as(unbakedPizza)
          .bake(unbakedPizza).in(preheatedOven, 15min)
          .return()
```
As you can see here, a single developer has the complete knowledge of the recipe and has coded that logic into a single uber workflow.

## Design Goals

Components of the Krystal Project try to adhere to the following design goals:

1. Separation of functional contracts and non-functional implementation details of dependencies: Developers coding a
   piece of functional business logic which depends on some other piece of business logic should be completely shielded
   from non-functional implementation details of the dependency as well as the runtime details of the environment in
   which the business logic is executing. Here non-functional requirements include:
    * Session-level Caching
    * Concurrency mode (multithreaded thread-per-task model, vs. reactive vs. hybrid model)
    * Batching and batch size of dependency inputs
1. Minimize lateral and upward impact of code changes: If all the business logic executing to fulfill a request is seen
   as a Directed acyclic graph, a code change being made in a node in the graph should not impact the implementation of
   any other node in the graph which is a direct descendant of it.
1. Zero glue code: If there is business logic A and business logic B, all the relevant business logic should be
   contained within the artefact which represents each piece of business logic.
1. Out-of the box Non-intrusive batching of service-call inputs
1. Optimal end-to-end execution: This programming model is designed to be adopted for executing complex business logic
   in systems that power features and user experiences which directly are accessed by end users of websites with heavy traffic, thus
   strongly needing a low-latency, high-throughput execution runtime.
    * Minimize use of native OS threads
    * Avoid the possibility of developers erroneously blocking on thread-blocking-code pre-maturely.
    * Streaming-over-network-connection capable
1. Avoid unnecessary bottlenecks and latency long poles: ... 
1. Developer friendliness:
    * Minimal bootstrapping overhead
    * Dependency discovery
    * Code generation
    * Single point of coding - One-to-one atomic mapping of coding units and functional requirements.
    * Minimize mental-overhead and avoid anti patterns inherent to reactive coding
    * Programming-language-native developer experience: For example, type safety.
1. Observability - application owners get this Out of the Box:
    * Circuit Breaking
    * Live Service Call Dashboards
    * Degradation levers
    * Metrics
    * Logging
    * DAG visualization of a request
1. Testability
    * Backward-incompatibility detection
    * Mocking-out-of-the box
    * Declarative unit test definition
    * Auto-generated unit test code/templates
1. Programming language agnostic spec definitions: Although the project currently supports the java programming language, keeping
    the programming-model spec language-agnostic allows the programming model to have implementations in different
    languages and allows application owners/feature-developers to choose a language of their choice.
