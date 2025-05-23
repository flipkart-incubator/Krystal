## Scope

This document describes the models that are auto-generated by Krystal and their function and design.

When a developer writes a vajramDef, the Vajram code generation annotation processor generates a few
classes which model the facetValues of the vajramDef. These models are of these types:

* Request
    * This object contains only and all [
      `@Input`](../vajramDef/vajramDef-java-sdk/src/main/java/com/flipkart/krystal/vajramDef/facets/Input.java)
      facetValues of the vajramDef. It is intended to be usable as an independant object by application
      developers (Ex: when
      invoking a vajramDef using the [
      `KrystalVajramExecutor`](../vajramDef/vajramDef-krystex/src/main/java/com/flipkart/krystal/vajramexecutor/krystex/KrystexVajramExecutor.java)
      or in some complex input resolvers which set inputs into Request Builders)
* Facets
    * This object contains all the Facets of the vajramDef.
    * It is not intended to be used by application developers directly. Instead it acts as a
      state holder for the facetValues values as
      they are computed during the lifecycle of the vajramDef's execution.
    * In older versions of Krystal, this was done using simple Maps which contained the facet name
      as key and facet value
      as the value. Since Krystal 9, this facetValues class is generated and used instead as getting and
      setting fields in objects takes lesser CPU cycles and is lighter on memory footprint.
* Batch Facets (Only applicable if batching is enabled)
    * This object contains values of all facets which have been configured to be batched together
      across vajramDef invocations, i.e. annotated with [
      `@Batch(forOutputLogic = true)`](../vajramDef/vajramDef-java-sdk/src/main/java/com/flipkart/krystal/vajramDef/batching/Batch.java)
    * Unlike the other the Request and Facets classes which primiarily used for communication across
      vajrams (maybe over the wire), persistance of facet values (in case of async orchestration) or
      for internal platform use, the Batch Facets class is excusilvely generated so that the
      application developer can access all the Batched facets which have been marked as
      `@Batch(forOutputLogic = true)`
* Common Facets (Only applicable if batching is enabled)
    * This object contains all facetValues which are not tagged with `@Batch`. These are those facetValues
      which remain same across the various batches. In Query Language terminology they are analogous
      the "group by" operation.

## Class Design

Each of these four categories of objects actually correspond to 3 interfaces:

* An Interface
* An Immuatable final class
* An Mutable builder for the Immutable class 

This design allows code to 