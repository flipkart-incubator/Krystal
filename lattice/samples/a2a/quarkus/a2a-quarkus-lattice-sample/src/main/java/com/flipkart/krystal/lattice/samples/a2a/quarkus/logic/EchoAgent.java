package com.flipkart.krystal.lattice.samples.a2a.quarkus.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

/** Echoes the user's input back as the agent response. */
@InvocableOutsideGraph
@Vajram
public abstract class EchoAgent extends ComputeVajramDef<String> {

  interface _Inputs {
    /** The text sent by the user in the A2A task message. */
    @IfAbsent(FAIL)
    String userInput();
  }

  @Output
  static String echo(String userInput) {
    return "Echo: " + userInput;
  }
}
