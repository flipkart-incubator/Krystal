package com.flipkart.krystal.vajram.samples.customer_service;

import static com.flipkart.krystal.traits.matchers.InputValueMatcher.equalsEnum;
import static com.flipkart.krystal.traits.matchers.InputValueMatcher.isAnyValue;
import static com.flipkart.krystal.traits.matchers.InputValueMatcher.isInstanceOf;
import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType.*;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType.L1;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType.L2;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req.agentType_s;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req.initialCommunication_s;
import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.dispatchTrait;
import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.*;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig.KrystexVajramExecutorConfigBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerServiceAgentPredicateTest {
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", 4);
  }

  private VajramGraph graph;
  private KrystexGraphBuilder kGraph;
  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();

    // Build the graph with all the vajram implementations
    graph =
        VajramGraph.builder().loadFromPackage(CustomerServiceAgent.class.getPackageName()).build();
    this.kGraph = KrystexGraph.builder().vajramGraph(graph);

    // Create and register dispatch policy
    kGraph.traitDispatchPolicies(
        new TraitDispatchPolicies(
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
                        .to(DefaultCustomerServiceAgent_Req.class))));
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void testL1CallAgent() {
    // Create request for L1 agent and Call communication
    CustomerServiceAgent_ReqImmut request =
        CustomerServiceAgent_ReqImmutPojo._builder()
            .agentType(L1)
            .initialCommunication(new Call("Recording content"))
            .customerName("Mowgli")
            ._build();

    // Execute and verify
    CompletableFuture<@Nullable String> result;
    try (var executor = kGraph.build().createExecutor(getExecutorConfig())) {
      result = executor.execute(request);
    }

    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("L1 Agent");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("Mowgli");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("received your call");
  }

  @Test
  void testL1EmailAgent() {
    CustomerServiceAgent_ReqImmut request =
        CustomerServiceAgent_ReqImmutPojo._builder()
            .agentType(L1)
            .initialCommunication(new Email("Email content"))
            .customerName("Pikachu")
            ._build();

    CompletableFuture<@Nullable String> result;
    try (var executor = kGraph.build().createExecutor(getExecutorConfig())) {
      result = executor.execute(request);
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("L1 Agent");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("Pikachu");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("received your email");
  }

  @Test
  void testL2CallAgent() {
    CustomerServiceAgent_ReqImmut request =
        CustomerServiceAgent_ReqImmutPojo._builder()
            .agentType(L2)
            .initialCommunication(new Call("Recording content"))
            .customerName("Swami")
            ._build();

    CompletableFuture<@Nullable String> result;
    try (var executor = kGraph.build().createExecutor(getExecutorConfig())) {
      result = executor.execute(request);
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("L2 Agent");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("Swami");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("received your call");
  }

  @Test
  void testL3EmailAgent() {
    CustomerServiceAgent_ReqImmut request =
        CustomerServiceAgent_ReqImmutPojo._builder()
            .agentType(L3)
            .initialCommunication(new Email("Email content"))
            .customerName("Gangadhar Vidhyadhar Mayadhar Omkarnath Shastri")
            ._build();

    CompletableFuture<@Nullable String> result;
    try (var executor = kGraph.build().createExecutor(getExecutorConfig())) {
      result = executor.execute(request);
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("L3 Agent");
    assertThat(result)
        .succeedsWithin(TEST_TIMEOUT)
        .asString()
        .contains("Gangadhar Vidhyadhar Mayadhar Omkarnath Shastri");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("received your email");
  }

  @Test
  void testDefaultEmailAgent() {
    // L2 agent with Email has no specific handler
    CustomerServiceAgent_ReqImmut request =
        CustomerServiceAgent_ReqImmutPojo._builder()
            .agentType(L2)
            .initialCommunication(new Email("Email content"))
            .customerName("Jejamma")
            ._build();

    CompletableFuture<@Nullable String> result;
    try (var executor = kGraph.build().createExecutor(getExecutorConfig())) {
      result = executor.execute(request);
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("I am an email Agent");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("Jejamma");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("received your email");
  }

  @Test
  void testDefaultCallAgent() {
    // L2 agent with Email has no specific handler
    CustomerServiceAgent_ReqImmut request =
        CustomerServiceAgent_ReqImmutPojo._builder()
            .agentType(L3)
            .initialCommunication(new Call("Call recording"))
            .customerName("Man without a name")
            ._build();

    CompletableFuture<@Nullable String> result;
    try (var executor = kGraph.build().createExecutor(getExecutorConfig())) {
      result = executor.execute(request);
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("I am a call Agent");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("Man without a name");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("received your call");
  }

  @Test
  void testFallbackForTicket() {
    // No specific handler for Ticket type
    CustomerServiceAgent_ReqImmut request =
        CustomerServiceAgent_ReqImmutPojo._builder()
            .agentType(L2)
            .initialCommunication(new Ticket("Ticket summary"))
            .customerName("Bruce Wayne")
            ._build();

    CompletableFuture<@Nullable String> result;
    try (var executor = kGraph.build().createExecutor(getExecutorConfig())) {
      result = executor.execute(request);
    }
    assertThat(result)
        .succeedsWithin(TEST_TIMEOUT)
        .asString()
        .contains("I am a Customer Service Agent");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("Bruce Wayne");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("received your Ticket");
  }

  private KrystexVajramExecutorConfigBuilder getExecutorConfig() {
    return KrystexVajramExecutorConfig.builder()
        .kryonExecutorConfig(
            KryonExecutorConfig.builder().executorService(executorLease.get()).build());
  }
}
