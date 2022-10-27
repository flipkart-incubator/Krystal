package com.flipkart.krystal.krystex;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KrystalTaskExecutorTest {

  private KrystalTaskExecutor krystalTaskExecutor;
  private NodeDefinitionRegistry nodeDefinitionRegistry;

  @BeforeEach
  void setUp() {
    this.nodeDefinitionRegistry = new NodeDefinitionRegistry();
    this.krystalTaskExecutor = new KrystalTaskExecutor(nodeDefinitionRegistry);
  }

  @Test
  void addNode_noDependencies_success()
      throws ExecutionException, InterruptedException, TimeoutException {
    NonBlockingNodeDefinition<String> nodeDefinition =
        new NonBlockingNodeDefinition<>("addNode_noDependencies_success_nodeName", emptySet()) {
          @Override
          protected String nonBlockingLogic() {
            return "computed_value";
          }
        };
    nodeDefinitionRegistry.add(nodeDefinition);
    Result<String> result = krystalTaskExecutor.requestExecution(nodeDefinition);
    assertEquals("computed_value", result.future().get(10, TimeUnit.SECONDS));
  }

  @Test
  void shutdown_preventsNewExecutionRequests() {
    krystalTaskExecutor.shutdown();
    assertThrows(
        IllegalStateException.class,
        () ->
            krystalTaskExecutor.requestExecution(
                new NonBlockingNodeDefinition<String>(
                    "shutdown_preventsNewExecutionRequests", emptySet()) {
                  @Override
                  protected String nonBlockingLogic() {
                    return "";
                  }
                }));
  }
}
