package com.flipkart.krystal.vajram.samples.chess;

import static com.flipkart.krystal.traits.matchers.InputValueMatcher.equalsEnum;
import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static com.flipkart.krystal.vajram.samples.chess.GetPiece_Req.type_s;
import static com.flipkart.krystal.vajram.samples.chess.PieceType.KNIGHT;
import static com.flipkart.krystal.vajram.samples.chess.PieceType.ROOK;
import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.dispatchTrait;
import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetPieceTest {

  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", 4);
  }

  private VajramGraph graph;
  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();

    // Build the graph with all the vajram implementations
    graph = VajramGraph.builder().loadFromPackage(GetPiece.class.getPackageName()).build();

    // Create and register dispatch policy
    graph.registerTraitDispatchPolicies(
        dispatchTrait(GetPiece_Req.class, graph)
            .conditionally(
                when(type_s, equalsEnum(KNIGHT)).to(GetKnight_Req.class),
                when(type_s, equalsEnum(ROOK)).to(GetRook_Req.class)));
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void getPiece_KnightType_returnsKnight() {
    CompletableFuture<Knight> result;
    try (var executor = graph.createExecutor(getExecutorConfig())) {
      result = executor.execute(GetPiece_ReqImmutPojo.<Knight>_builder().type(KNIGHT)._build());
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT).isEqualTo(new Knight());
  }

  @Test
  void getPiece_RookType_returnsRook() {
    CompletableFuture<Rook> result;
    try (var executor = graph.createExecutor(getExecutorConfig())) {
      result = executor.execute(GetPiece_ReqImmutPojo.<Rook>_builder().type(ROOK)._build());
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT).isEqualTo(new Rook());
  }

  private KrystexVajramExecutorConfig getExecutorConfig() {
    return KrystexVajramExecutorConfig.builder()
        .kryonExecutorConfigBuilder(
            KryonExecutorConfig.builder().executorService(executorLease.get()))
        .build();
  }
}
