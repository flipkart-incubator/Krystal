# Polymorphism in Krystal: Traits and Dispatch Mechanisms

## Introduction to Traits

In Krystal, a trait defines a functional behavior without implementing business logic, similar to
interfaces in traditional OOP languages. Traits specify the semantics of inputs and outputs while
allowing different implementations to fulfill the contract.

Traits are written just like Vajrams - they are written as abstract classes which have a `_Facets`
class containing facet definitions. The differences being:

* Traits can only have input facets (no injections, depdendencies etc)
* Traits cannot have input resolvers or output logic.
* Traits are defined by the `@Trait` annotation. and extend the `TraitDef` class (instead of
  `ComputeVajramDef` or `IOVajramDef`)

Examples of traits:

* [MultiAdd](../vajram/vajram-samples/src/main/java/com/flipkart/krystal/vajram/samples/calculator/add/MultiAdd.java)
* [CustomerServiceAgent](../vajram/vajram-samples/src/main/java/com/flipkart/krystal/vajram/samples/customer_service/CustomerServiceAgent.java)

## Constraints

* A trait can have multiple vajrams conforming to it but nne vajram can conform to at most one Trait
* A vajram can have only those inputs that are defined in the trait. Currently it cannot add inputs
  of its own
* The data types of the inputs of a vajram should exactly match the data types of the inputs of the
  trait it conforms to.
* Currently the data type of the output of a vajram should exactly match the data type of the output
  of the trait it conforms to.

## Conforming Vajrams to Traits

Vajrams can implement traits using the `@ConformsToTrait` annotation:

Examples of vajrams conforming to traits:

* [ChainAdd](../vajram/vajram-samples/src/main/java/com/flipkart/krystal/vajram/samples/calculator/add/ChainAdd.java) [@ConformsToTrait](../vajram/vajram-java-sdk/src/main/java/com/flipkart/krystal/vajram/annos/ConformsToTrait.java) [MultiAdd](../vajram/vajram-samples/src/main/java/com/flipkart/krystal/vajram/samples/calculator/add/MultiAdd.java)
* [L1CallAgent](../vajram/vajram-samples/src/main/java/com/flipkart/krystal/vajram/samples/customer_service/L1CallAgent.java) [@ConformsToTrait](../vajram/vajram-java-sdk/src/main/java/com/flipkart/krystal/vajram/annos/ConformsToTrait.java) [CustomerServiceAgent](../vajram/vajram-samples/src/main/java/com/flipkart/krystal/vajram/samples/customer_service/CustomerServiceAgent.java)

## Trait Dispatch Mechanisms

Krystal supports two powerful dispatch mechanisms:

### 1. [Static Dispatch](https://en.wikipedia.org/wiki/Static_dispatch)

Static dispatch uses a dependency injection approach where trait implementations are bound
statically based on qualifiers.

- **How it works**: When a vajram depends on a trait, a qualifier annotation can be used to
  influence which concrete implementation is selected
- **Performance**: O(1) lookup time with minimal memory overhead
- **Use case**: When the implementation choice is known at configuration time and doesn't depend on
  runtime values

Static dispatch configurations are defined via [
`StaticDispatchPolicy`](../krystal-common/src/main/java/com/flipkart/krystal/traits/StaticDispatchPolicy.java)

Example of static dispatch can be seen
in [AddUsingTraits](../vajram/vajram-samples/src/main/java/com/flipkart/krystal/vajram/samples/calculator/add/AddUsingTraits.java)
and its test
case [AddUsingTraitsTest](../vajram/vajram-samples/src/test/java/com/flipkart/krystal/vajram/samples/calculator/add/AddUsingTraitsTest.java)

### 2. [Dynamic Predicate Dispatch](https://en.wikipedia.org/wiki/Predicate_dispatch)

Dynamic dispatch determines the implementation based on pattern matching against input values at
runtime.

- **How it works**: Input values are evaluated against predicate patterns to select the correct
  implementation
- **Performance**: O(n*m) where n is the number of inputs used for matching and m is the number of
  dispatch cases
- **Use case**: When implementation selection depends on runtime input values

Example of static dispatch can be seen
in [MultiAgentContact](../vajram/vajram-samples/src/main/java/com/flipkart/krystal/vajram/samples/customer_service/MultiAgentContact.java)
and the test
cases [MultiAgentContactTest](../vajram/vajram-samples/src/test/java/com/flipkart/krystal/vajram/samples/customer_service/MultiAgentContactTest.java)
and [CustomerServiceAgentTest](../vajram/vajram-samples/src/test/java/com/flipkart/krystal/vajram/samples/customer_service/CustomerServiceAgentTest.java)

## Dispatch Process

When a trait is invoked, Krystal:

1. Identifies that the target is a trait
2. Retrieves the appropriate dispatch policy
3. For static dispatch:
    - Determines the bound vajram using the dependency qualifier
    - Redirects the invocation to the bound vajram
4. For predicate dispatch:
    - Evaluates input values against configured patterns
    - Routes requests to matching implementations
    - Merges responses from multiple implementations if needed

## Benefits of Krystal's Polymorphism

1. **Separation of interface and implementation**: Traits define behavior contracts without
   implementation details
2. **Flexibility**: Dynamic selection of implementations based on runtime conditions
3. **Testability**: Easy to mock or substitute implementations
4. **Extensibility**: Add new implementations without modifying existing code

## Best Practices

- Use static dispatch when possible for better performance
- Reserve dynamic dispatch for cases where implementation selection truly depends on runtime values
- Keep predicate conditions simple and maintainable
- Document dispatch policies clearly to avoid confusion

The powerful combination of traits and flexible dispatch mechanisms makes Krystal's polymorphism
system both powerful and practical for complex application requirements.