package com.flipkart.krystal.vajram.samples.customer_service;

import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req.agentType_n;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req.customerName_n;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req.initialCommunication_n;
import static com.flipkart.krystal.vajram.samples.customer_service.MultiAgentContact_Fac.responses_n;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.data.IfNoValue;
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

@Vajram
@ExternallyInvocable
abstract class MultiAgentContact extends ComputeVajramDef<List<String>> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {
    @IfNoValue
    @Input String name;
    @IfNoValue
    @Input String communication;

    @IfNoValue
    @Dependency(onVajram = CustomerServiceAgent.class, canFanout = true)
    String responses;
  }

  @Resolve(
      dep = responses_n,
      depInputs = {customerName_n, agentType_n, initialCommunication_n})
  static FanoutCommand<CustomerServiceAgent_ImmutReq.Builder> sendCommunications(
      String name, String communication) {
    List<CustomerServiceAgent_ImmutReq.Builder> result = new ArrayList<>();
    List<Function<String, InitialCommunication>> communicationBuilders =
        List.of(Call::new, Email::new, Ticket::new);
    for (AgentType agentType : AgentType.values()) {
      for (Function<String, InitialCommunication> communicationBuilder : communicationBuilders) {
        result.add(
            CustomerServiceAgent_ImmutReqPojo._builder()
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
