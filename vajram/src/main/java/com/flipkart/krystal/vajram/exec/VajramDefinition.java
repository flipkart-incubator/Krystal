package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.vajram.exec.Vajrams.getVajramDefClass;

import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.facets.resolution.InputResolverDefinition;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Accessors(fluent = true)
@Getter
public final class VajramDefinition {

  private final Vajram<?> vajram;

  private final ImmutableCollection<InputResolverDefinition> inputResolverDefinitions;

  private final ElementTags outputLogicTags;
  private final ElementTags vajramTags;

  private final VajramMetadata vajramMetadata;

  private final Class<? extends Vajram<?>> vajramDefClass;

  private final VajramID vajramId;

  public VajramDefinition(Vajram<?> vajram) {
    this.vajram = vajram;
    this.vajramId = Vajrams.parseVajramId(vajram);
    this.vajramDefClass = getVajramDefClass(vajram.getClass());
    this.inputResolverDefinitions = ImmutableList.copyOf(Vajrams.parseInputResolvers(vajram));
    this.outputLogicTags = Vajrams.parseOutputLogicTags(vajram);
    this.vajramTags = Vajrams.parseVajramTags(vajramId, vajram);
    this.vajramMetadata = new VajramMetadata(vajram);
  }
}
