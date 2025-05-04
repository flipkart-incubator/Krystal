package com.flipkart.krystal.vajram.samples.customer_service;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.ComputeDelegationMode.NONE;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.traits.UseForDispatch;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitRoot;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;

/**
 * Sample trait demonstrating dynamic predicate dispatch capability of krystal
 *
 * <p>Takes in a customer Communication an responds to it
 */
@Trait
@CallGraphDelegationMode(NONE)
@ExternallyInvocable
public interface CustomerServiceAgent extends TraitRoot<String> {
  @SuppressWarnings("initialization.field.uninitialized")
  class _Inputs {
    @IfAbsent(FAIL)
    @UseForDispatch
    AgentType agentType;

    @IfAbsent(FAIL)
    @UseForDispatch
    InitialCommunication initialCommunication;

    @IfAbsent(FAIL)
    @UseForDispatch
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
