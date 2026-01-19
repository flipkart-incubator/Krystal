package com.flipkart.krystal.lattice.ext.mcp;

import static java.util.concurrent.CompletableFuture.failedFuture;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.ext.mcp.api.McpServer;
import com.flipkart.krystal.lattice.krystex.KrystexDopant;
import com.flipkart.krystal.lattice.vajram.VajramRequestExecutionContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletionStage;
import org.jspecify.annotations.NonNull;

@Singleton
@DopantType(McpServerDopant.MCP_SERVER_DOPANT_TYPE)
public final class McpServerDopant implements Dopant<McpServer, NoConfiguration> {

  static final String MCP_SERVER_DOPANT_TYPE = "krystal.lattice.mcpServer";

  private final KrystexDopant krystexDopant;

  @Inject
  public McpServerDopant(KrystexDopant krystexDopant) {
    this.krystexDopant = krystexDopant;
  }

  public <T> CompletionStage<T> executeMcpTool(ImmutableRequest<T> vajramRequest) {
    return executeVajram(vajramRequest);
  }

  private <T> @NonNull CompletionStage<T> executeVajram(ImmutableRequest<T> vajramRequest) {
    try {
      return krystexDopant.executeRequest(
          VajramRequestExecutionContext.<T>builder().vajramRequest(vajramRequest).build());
    } catch (Exception e) {
      return failedFuture(e);
    }
  }

  public <T> CompletionStage<T> executeMcpResource(ImmutableRequest<T> vajramRequest) {
    return executeVajram(vajramRequest);
  }
}
