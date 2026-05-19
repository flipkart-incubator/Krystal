package com.flipkart.krystal.lattice.samples.a2a.quarkus.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

/** Acknowledges cancellation of an EchoAgent task. */
@InvocableOutsideGraph
@Vajram
public abstract class EchoAgentCanceller extends ComputeVajramDef<String> {

  interface _Inputs {
    /** The ID of the task being cancelled. */
    @IfAbsent(FAIL)
    String taskId();
  }

  @Output
  static String confirm(String taskId) {
    return "Cancelled task: " + taskId;
  }
}
