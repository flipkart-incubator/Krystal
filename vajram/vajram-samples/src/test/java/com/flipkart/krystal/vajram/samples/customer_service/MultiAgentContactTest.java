package com.flipkart.krystal.vajram.samples.customer_service;

import static com.flipkart.krystal.traits.matchers.InputValueMatcher.equalsEnum;
import static com.flipkart.krystal.traits.matchers.InputValueMatcher.isAnyValue;
import static com.flipkart.krystal.traits.matchers.InputValueMatcher.isInstanceOf;
import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType.L1;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType.L2;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType.L3;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req.agentType_s;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req.initialCommunication_s;
import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.dispatchTrait;
import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.Call;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.Email;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.assertj.core.api.ListAssert;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultiAgentContactTest {
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", 4);
  }

  private VajramKryonGraph graph;
  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();

    // Build the graph with all the vajram implementations
    graph =
        VajramKryonGraph.builder()
            .loadFromPackage(MultiAgentContact.class.getPackageName())
            .build();

    VajramID traitId = graph.getVajramIdByVajramDefType(CustomerServiceAgent.class);

    // Get the facets to use for dispatch

    // Create and register dispatch policy
    graph.registerTraitDispatchPolicies(
        dispatchTrait(CustomerServiceAgent_Req.class, graph)
            .conditionally(
                when(agentType_s, equalsEnum(L1))
                    .and(initialCommunication_s, isInstanceOf(Call.class))
                    .to(L1CallAgent_Req.class),
                when(agentType_s, equalsEnum(L1))
                    .and(initialCommunication_s, isInstanceOf(Email.class))
                    .to(L1EmailAgent_Req.class),
                when(agentType_s, equalsEnum(L2))
                    .and(initialCommunication_s, isInstanceOf(Call.class))
                    .to(L2CallAgent_Req.class),
                when(agentType_s, equalsEnum(L3))
                    .and(initialCommunication_s, isInstanceOf(Email.class))
                    .to(L3EmailAgent_Req.class),
                when(initialCommunication_s, isInstanceOf(Call.class))
                    .to(DefaultCallAgent_Req.class),
                when(initialCommunication_s, isInstanceOf(Email.class))
                    .to(DefaultEmailAgent_Req.class),
                // Default fallback
                when(agentType_s, isAnyValue())
                    .and(initialCommunication_s, isAnyValue())
                    .to(DefaultCustomerServiceAgent_Req.class)));
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void dynamicDispatch_multipleDispatchTargets_success() {
    // Create request for L1 agent and Call communication
    MultiAgentContact_ReqImmut request =
        MultiAgentContact_ReqImmutPojo._builder()
            .name("Bahubali")
            .communication("Jai Mahishmati")
            ._build();

    // Execute and verify
    CompletableFuture<@Nullable List<String>> responses;
    try (var executor = graph.createExecutor(getExecutorConfig()); ) {
      responses = executor.execute(request);
    }

    ListAssert<Object> responsesList =
        assertThat(responses).succeedsWithin(TEST_TIMEOUT).asInstanceOf(LIST);
    responsesList.hasSize(9);
    responsesList.allSatisfy(
        response -> assertThat(response).asInstanceOf(STRING).contains("Bahubali"));
    responsesList.filteredOn(o -> o instanceof String s && s.contains("L1 Agent")).hasSize(2);
    responsesList.filteredOn(o -> o instanceof String s && s.contains("L2 Agent")).hasSize(1);
    responsesList.filteredOn(o -> o instanceof String s && s.contains("L3 Agent")).hasSize(1);
    responsesList
        .filteredOn(o -> o instanceof String s && s.contains("I am a call Agent"))
        .hasSize(1);
    responsesList
        .filteredOn(o -> o instanceof String s && s.contains("I am an email Agent"))
        .hasSize(1);
    responsesList
        .filteredOn(o -> o instanceof String s && s.contains("I am a Customer Service Agent"))
        .hasSize(3);
    responsesList
        .filteredOn(o -> o instanceof String s && s.contains("received your call"))
        .hasSize(3);
    responsesList
        .filteredOn(o -> o instanceof String s && s.contains("received your email"))
        .hasSize(3);
    responsesList
        .filteredOn(o -> o instanceof String s && s.contains("received your Ticket"))
        .hasSize(3);
  }

  private KrystexVajramExecutorConfig getExecutorConfig() {
    return KrystexVajramExecutorConfig.builder()
        .kryonExecutorConfigBuilder(
            KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()))
        .build();
  }
}
