package com.flipkart.krystal.vajram.inputs.resolution;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.vajram.inputs.QualifiedInputs;
import com.flipkart.krystal.vajram.inputs.VajramDependencyTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import java.util.List;

abstract class AbstractSimpleInputResolver extends AbstractInputResolver {

  AbstractSimpleInputResolver(
      VajramDependencyTypeSpec<?, ?, ?, ?> dependency,
      VajramInputTypeSpec<?, ?> targetInput,
      List<VajramInputTypeSpec<?, ?>> sourceInputs) {
    super(
        sourceInputs.stream().map(VajramInputTypeSpec::name).collect(toImmutableSet()),
        new QualifiedInputs(dependency.name(), targetInput.name()));
  }
}
