package com.flipkart.krystal.lattice.samples.a2a.quarkus.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

/** Reverses the characters of the user's input text. */
@InvocableOutsideGraph
@Vajram
public abstract class ReverseAgent extends ComputeVajramDef<String> {

  interface _Inputs {
    /** The text sent by the user in the A2A task message. */
    @IfAbsent(FAIL)
    String userInput();
  }

  @Output
  static String reverse(String userInput) {
    return new StringBuilder(userInput).reverse().toString();
  }
}
