package com.flipkart.krystal.model;

/**
 * This interface provides a standard for designing data model classes. All vajram related models
 * including Requests, FacetValues, Responses, Batch Items, Common, etc implement this interface.
 *
 * <p>Every model MUST be represented by a Model Interface with two sub-interfaces - ImmutableModel
 * and ImmutableModelBuilder. The immutable model builder can be used to build the corresponding
 * immutable model using the {@link #_build()} method. And the immutable model can be converted to a
 * builder via {@link #_asBuilder()}. These interfaces can be implemented in various ways based on
 * the requirements. For example, a "Pojo" implementation consists of all the data as class level
 * fields. A serializable implementation may wrap a byte array and/or other serializable objects
 */
@SuppressWarnings("ClassReferencesSubclass") // By Design
public interface Model {
  ImmutableModel.Builder _asBuilder();

  ImmutableModel _build();

  Model _newCopy();
}
