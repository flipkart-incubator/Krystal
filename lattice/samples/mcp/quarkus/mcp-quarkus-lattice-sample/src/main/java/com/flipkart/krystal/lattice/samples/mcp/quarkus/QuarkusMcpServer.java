package com.flipkart.krystal.lattice.samples.mcp.quarkus;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategy.POOLED_NATIVE_THREAD_PER_REQUEST;

import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.lattice.core.doping.DopeWith;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec.ThreadingStrategySpecBuilder;
import com.flipkart.krystal.lattice.ext.cdi.CdiFramework;
import com.flipkart.krystal.lattice.ext.mcp.McpServerDopantSpec;
import com.flipkart.krystal.lattice.ext.mcp.McpServerDopantSpec.McpServerDopantSpecBuilder;
import com.flipkart.krystal.lattice.ext.mcp.api.McpServer;
import com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationSpec;
import com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationSpec.QuarkusApplicationSpecBuilder;
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec;
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec.KrystexDopantSpecBuilder;
import com.flipkart.krystal.lattice.samples.mcp.quarkus.logic.AddSecretNumber;
import com.flipkart.krystal.lattice.samples.mcp.quarkus.logic.CapitalizeString;
import com.flipkart.krystal.lattice.samples.mcp.quarkus.logic.GetMultiplicationTable;
import com.flipkart.krystal.lattice.samples.mcp.quarkus.logic.GetPreambleOfIndia;
import com.flipkart.krystal.lattice.samples.mcp.quarkus.logic.HelloUserWithUri;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec.VajramDopantSpecBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;

@LatticeApp(
    description = "A sample MCP Server powered by quarkus",
    dependencyInjectionFramework = CdiFramework.class)
@McpServer(
    toolVajrams = {CapitalizeString.class, AddSecretNumber.class, GetMultiplicationTable.class},
    resourceVajrams = GetPreambleOfIndia.class,
    resourceTemplateVajrams = HelloUserWithUri.class)
public abstract class QuarkusMcpServer extends LatticeApplication {
  @DopeWith
  public static ThreadingStrategySpecBuilder threading() {
    return ThreadingStrategySpec.builder().threadingStrategy(POOLED_NATIVE_THREAD_PER_REQUEST);
  }

  @DopeWith
  public static VajramDopantSpecBuilder vajramGraph() {
    return VajramDopantSpec.builder()
        .vajramGraphBuilder(
            VajramGraph.builder()
                .loadFromPackage("com.flipkart.krystal.lattice.samples.mcp.quarkus.logic"));
  }

  @DopeWith
  public static KrystexDopantSpecBuilder krystex() {
    return KrystexDopantSpec.builder();
  }

  @DopeWith
  public static McpServerDopantSpecBuilder mcpServer() {
    return McpServerDopantSpec.builder();
  }

  @DopeWith
  public static QuarkusApplicationSpecBuilder quarkusApp() {
    return QuarkusApplicationSpec.builder();
  }
}
