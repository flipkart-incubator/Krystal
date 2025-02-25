package com.flipkart.krystal.vajramexecutor.krystex.traitbinding;

import com.flipkart.krystal.vajram.KrystalElement.VajramTrait;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramTraitDef;

public record TraitBinding(
    Class<? extends VajramTraitDef<?>> trait, Anno anno, Class<? extends VajramDef<?>> vajram) {}
