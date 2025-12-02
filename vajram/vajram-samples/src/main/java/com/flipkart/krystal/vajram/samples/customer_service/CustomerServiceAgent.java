package com.flipkart.krystal.vajram.samples.customer_service;

import static com.flipkart.krystal.annos.ComputeDelegationMode.NONE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.traits.UseForPredicateDispatch;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;

/**
 * Sample trait demonstrating dynamic predicate dispatch capability of krystal
 *
 * <p>Takes in a customer Communication and responds to it.
 */
@Trait
@CallGraphDelegationMode(NONE)
@InvocableOutsideGraph
public interface CustomerServiceAgent extends TraitDef<String> {
  @SuppressWarnings("initialization.field.uninitialized")
  class _Inputs {
    @IfAbsent(FAIL)
    @UseForPredicateDispatch
    AgentType agentType;

    @IfAbsent(FAIL)
    @UseForPredicateDispatch
    InitialCommunication initialCommunication;

    @IfAbsent(FAIL)
    @UseForPredicateDispatch
    String customerName;
  }

  enum AgentType {
    L1,
    L2,
    L3,
  }

  sealed interface InitialCommunication {}

  record Email(String emailContent) implements InitialCommunication {}

  record Call(String callRecording) implements InitialCommunication {}

  record Ticket(String ticketSummary) implements InitialCommunication {}
}
