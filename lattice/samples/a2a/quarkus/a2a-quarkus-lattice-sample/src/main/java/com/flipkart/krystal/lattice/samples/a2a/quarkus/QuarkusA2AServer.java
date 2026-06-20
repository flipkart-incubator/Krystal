package com.flipkart.krystal.lattice.samples.a2a.quarkus;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategy.POOLED_NATIVE_THREAD_PER_REQUEST;

import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.lattice.core.doping.DopeWith;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec.ThreadingStrategySpecBuilder;
import com.flipkart.krystal.lattice.ext.a2a.A2AServerDopantSpec;
import com.flipkart.krystal.lattice.ext.a2a.A2AServerDopantSpec.A2AServerDopantSpecBuilder;
import com.flipkart.krystal.lattice.ext.a2a.api.A2AServer;
import com.flipkart.krystal.lattice.ext.a2a.api.Agent;
import com.flipkart.krystal.lattice.ext.a2a.api.AgentSkill;
import com.flipkart.krystal.lattice.ext.cdi.CdiFramework;
import com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationSpec;
import com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationSpec.QuarkusApplicationSpecBuilder;
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec;
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec.KrystexDopantSpecBuilder;
import com.flipkart.krystal.lattice.samples.a2a.quarkus.logic.EchoAgent;
import com.flipkart.krystal.lattice.samples.a2a.quarkus.logic.EchoAgentCanceller;
import com.flipkart.krystal.lattice.samples.a2a.quarkus.logic.ReverseAgent;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec.VajramDopantSpecBuilder;

/**
 * Sample A2A server powered by Quarkus demonstrating two agent skills:
 *
 * <ul>
 *   <li>{@code echo} – echoes the user's input back (with optional cancellation)
 *   <li>{@code reverse} – reverses the characters of the user's input (no explicit canceller)
 * </ul>
 *
 * <p>The {@code AgentCard} is provided by {@link SampleAgentCardProducer} and is served
 * automatically at {@code /.well-known/agent.json}.
 */
@LatticeApp(
    description = "A sample A2A Server powered by Quarkus",
    dependencyInjectionFramework = CdiFramework.class)
@A2AServer(
    agent =
        @Agent(
            skills = {
              @AgentSkill(
                  name = "echo",
                  executor = EchoAgent.class,
                  canceller = EchoAgentCanceller.class),
              @AgentSkill(name = "reverse", executor = ReverseAgent.class)
            }))
public abstract class QuarkusA2AServer extends LatticeApplication {

  @DopeWith
  public static ThreadingStrategySpecBuilder threading() {
    return ThreadingStrategySpec.builder().threadingStrategy(POOLED_NATIVE_THREAD_PER_REQUEST);
  }

  @DopeWith
  public static VajramDopantSpecBuilder vajramGraph() {
    return VajramDopantSpec.builder()
        .vajramGraphBuilder(
            VajramGraph.builder()
                .loadFromPackage("com.flipkart.krystal.lattice.samples.a2a.quarkus.logic"));
  }

  @DopeWith
  public static KrystexDopantSpecBuilder krystex() {
    return KrystexDopantSpec.builder();
  }

  @DopeWith
  public static A2AServerDopantSpecBuilder a2aServer() {
    return A2AServerDopantSpec.builder();
  }

  @DopeWith
  public static QuarkusApplicationSpecBuilder quarkusApp() {
    return QuarkusApplicationSpec.builder();
  }
}
