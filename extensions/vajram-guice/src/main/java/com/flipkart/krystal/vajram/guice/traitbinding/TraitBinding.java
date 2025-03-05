package com.flipkart.krystal.vajram.guice.traitbinding;

import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramTraitDef;
import com.google.inject.Key;
import java.util.function.Supplier;

public record TraitBinding(
    Key<? extends VajramTraitDef<?>> key, Supplier<Class<? extends VajramDef<?>>> vajram) {}
