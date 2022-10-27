package com.flipkart.krystal.vajram.inputs;

public sealed interface InputResolver permits DefaultInputResolver, ForwardingResolver,
    ForwardingResolverV2, StaticResolver {
  QualifiedInputId resolutionTarget();
}
