package com.flipkart.krystal.traits;

public sealed interface DynamicDispatchPolicy extends TraitDispatchPolicy
    permits PredicateDispatchPolicy, ComputeDispatchPolicy {}
