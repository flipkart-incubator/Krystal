package com.flipkart.krystal.vajramexecutor.krystex.traitbinding;

import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramTraitDef;
import java.util.List;
import lombok.Getter;

public class TraitBinder {

  @Getter private final List<TraitBinding> traitBindings;

  public TraitBinder(TraitBinding... traitBindings) {
    this.traitBindings = List.of(traitBindings);
  }

  public static AnnotatedBindingBuilder bindTrait(Class<? extends VajramTraitDef<?>> traitDef) {
    return new AnnotatedBindingBuilder(traitDef);
  }
}
