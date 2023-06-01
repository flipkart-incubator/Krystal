package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputSource;
import com.flipkart.krystal.vajram.inputs.InputTag;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public final class InputDef extends AbstractInput {
  private boolean needsModulation;
  private Set<InputSource> sources = ImmutableSet.of();
  private Map<InputTag, String> inputTags = ImmutableMap.of();

  @Override
  public Input<?> toInputDefinition() {
    return Input.builder()
        .name(getName())
        .type(toDataType())
        .mandatory(isMandatory())
        .needsModulation(isNeedsModulation())
        .documentation(getDoc())
        .sources(getSources())
        .inputTags(getInputTags())
        .build();
  }
}
