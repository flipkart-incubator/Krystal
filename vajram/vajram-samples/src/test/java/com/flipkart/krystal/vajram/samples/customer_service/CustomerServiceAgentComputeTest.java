package com.flipkart.krystal.vajram.samples.customer_service;

import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType.*;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType.L1;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType.L2;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.*;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.traits.ComputeDispatchPolicyImpl;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerServiceAgentComputeTest {
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
            .loadFromPackage(CustomerServiceAgent.class.getPackageName())
            .build();

    Map<
            AgentType,
            Map<Class<? extends InitialCommunication>, Class<? extends CustomerServiceAgent_Req>>>
        byAgent =
            ofEntries(
                entry(
                    L1,
                    ofEntries(
                        entry(Call.class, L1CallAgent_Req.class),
                        entry(Email.class, L1EmailAgent_Req.class))),
                entry(L2, ofEntries(entry(Call.class, L2CallAgent_Req.class))),
                entry(L3, ofEntries(entry(Email.class, L3EmailAgent_Req.class))));
    Map<Class<? extends InitialCommunication>, Class<? extends CustomerServiceAgent_Req>>
        defaultsByCommType =
            Map.of(
                Call.class, DefaultCallAgent_Req.class, Email.class, DefaultEmailAgent_Req.class);

    // Create and register dispatch policy
    Set<Class<? extends CustomerServiceAgent_Req>> dispatchTargets =
        new HashSet<>(
            Sets.union(
                byAgent.values().stream()
                    .map(Map::values)
                    .flatMap(Collection::stream)
                    .collect(toSet()),
                Set.copyOf(defaultsByCommType.values())));
    dispatchTargets.add(DefaultCustomerServiceAgent_Req.class);
    graph.registerTraitDispatchPolicies(
        new ComputeDispatchPolicyImpl<>(
            CustomerServiceAgent_Req.class,
            (dependency, request) -> {
              InitialCommunication initialCommunication = request.initialCommunication();
              var commType =
                  initialCommunication == null
                      ? CustomerServiceAgent_Req.class
                      : initialCommunication.getClass();
              return byAgent
                  .getOrDefault(request.agentType(), Map.of())
                  .getOrDefault(
                      commType,
                      defaultsByCommType.getOrDefault(
                          commType, DefaultCustomerServiceAgent_Req.class));
            },
            ImmutableSet.copyOf(dispatchTargets),
            graph));
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
    try (var executor = graph.createExecutor(getExecutorConfig()); ) {
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
    try (var executor = graph.createExecutor(getExecutorConfig()); ) {
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
    try (var executor = graph.createExecutor(getExecutorConfig()); ) {
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
    try (var executor = graph.createExecutor(getExecutorConfig()); ) {
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
    try (var executor = graph.createExecutor(getExecutorConfig())) {
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
    try (var executor = graph.createExecutor(getExecutorConfig()); ) {
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
    try (var executor = graph.createExecutor(getExecutorConfig()); ) {
      result = executor.execute(request);
    }
    assertThat(result)
        .succeedsWithin(TEST_TIMEOUT)
        .asString()
        .contains("I am a Customer Service Agent");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("Bruce Wayne");
    assertThat(result).succeedsWithin(TEST_TIMEOUT).asString().contains("received your Ticket");
  }

  private KrystexVajramExecutorConfig getExecutorConfig() {
    return KrystexVajramExecutorConfig.builder()
        .kryonExecutorConfigBuilder(
            KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()))
        .build();
  }
}
