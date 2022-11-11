package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.flipkart.krystal.vajram.inputs.QualifiedInputs;
import com.google.common.collect.ImmutableSet;

public record DefaultInputResolver(ImmutableSet<String> sources, QualifiedInputs resolutionTarget)
    implements InputResolver {}
