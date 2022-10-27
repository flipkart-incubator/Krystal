package com.flipkart.krystal.vajram.inputs;

import java.util.Set;
import java.util.function.Function;

public record DefaultInputResolver(Set<InputId> sources, QualifiedInputId resolutionTarget, Function<Object[],Object> resolutionLogic) implements InputResolver {


}
