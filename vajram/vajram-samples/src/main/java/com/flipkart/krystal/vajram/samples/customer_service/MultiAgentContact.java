package com.flipkart.krystal.vajram.samples.customer_service;

import static com.flipkart.krystal.data.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req.agentType_n;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req.customerName_n;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req.initialCommunication_n;
import static com.flipkart.krystal.vajram.samples.customer_service.MultiAgentContact_Fac.responses_n;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.IfAbsent;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.Call;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.Email;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.InitialCommunication;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.Ticket;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("initialization.field.uninitialized")
@ExternallyInvocable
@Vajram
abstract class MultiAgentContact extends ComputeVajramDef<List<String>> {
  static class _Inputs {
    @IfAbsent(FAIL)
    String name;

    @IfAbsent(FAIL)
    String communication;
  }

  static class _InternalFacets {
    @IfAbsent(FAIL)
    @Dependency(onVajram = CustomerServiceAgent.class, canFanout = true)
    String responses;
  }

  @Resolve(
      dep = responses_n,
      depInputs = {customerName_n, agentType_n, initialCommunication_n})
  static FanoutCommand<CustomerServiceAgent_ReqImmut.Builder> sendCommunications(
      String name, String communication) {
    List<CustomerServiceAgent_ReqImmut.Builder> result = new ArrayList<>();
    List<Function<String, InitialCommunication>> communicationBuilders =
        List.of(Call::new, Email::new, Ticket::new);
    for (AgentType agentType : AgentType.values()) {
      for (Function<String, InitialCommunication> communicationBuilder : communicationBuilders) {
        result.add(
            CustomerServiceAgent_ReqImmutPojo._builder()
                .agentType(agentType)
                .customerName(name)
                .initialCommunication(communicationBuilder.apply(communication)));
      }
    }
    return executeFanoutWith(result);
  }

  @Output
  static List<String> output(FanoutDepResponses<CustomerServiceAgent_Req, String> responses) {
    return ImmutableList.copyOf(
        responses.requestResponsePairs().stream()
            .map(RequestResponse::response)
            .map(Errable::valueOrThrow)
            .toList());
  }
}
