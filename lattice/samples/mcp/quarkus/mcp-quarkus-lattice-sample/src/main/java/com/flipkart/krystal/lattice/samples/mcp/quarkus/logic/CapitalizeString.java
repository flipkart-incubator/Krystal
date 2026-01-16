package com.flipkart.krystal.lattice.samples.mcp.quarkus.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

/** Capitalizes the english characters in a string */
@InvocableOutsideGraph
@Vajram
public abstract class CapitalizeString extends ComputeVajramDef<String> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    /** The string to capitalize */
    @IfAbsent(FAIL)
    String input;
  }

  @Output
  static String capitalize(String input) {
    return input.toUpperCase();
  }
}
