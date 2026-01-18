package com.flipkart.krystal.lattice.samples.mcp.quarkus.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

/** Adds a secret number to an input number */
@InvocableOutsideGraph
@Vajram
public abstract class AddSecretNumber extends ComputeVajramDef<Integer> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    /** The number to add the secret number */
    @IfAbsent(FAIL)
    int input;
  }

  @Output
  static int add(int input) {
    return input + 23445;
  }
}
