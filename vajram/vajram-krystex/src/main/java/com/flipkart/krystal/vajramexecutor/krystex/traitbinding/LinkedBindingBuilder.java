package com.flipkart.krystal.vajramexecutor.krystex.traitbinding;

import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramTraitDef;

public class LinkedBindingBuilder {

  private final Class<? extends VajramTraitDef<?>> traitDef;
  private final Anno anno;

  LinkedBindingBuilder(Class<? extends VajramTraitDef<?>> traitDef, Anno anno) {
    this.traitDef = traitDef;
    this.anno = anno;
  }

  public TraitBinding to(Class<? extends VajramDef<?>> vajramDef) {
    return new TraitBinding(traitDef, anno, vajramDef);
  }
}
