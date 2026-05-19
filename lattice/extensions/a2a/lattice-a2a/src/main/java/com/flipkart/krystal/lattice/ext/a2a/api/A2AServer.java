package com.flipkart.krystal.lattice.ext.a2a.api;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.lattice.core.LatticeApplication;
import java.lang.annotation.Retention;

/**
 * Indicates that the {@link LatticeApplication} on which this annotation is placed acts as an <a
 * href="https://a2a-protocol.org/latest/specification/">A2A Server</a>.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @LatticeApp(description = "My A2A Server", dependencyInjectionFramework = CdiFramework.class)
 * @A2AServer(agent =
 *    @Agent(skills = {
 *      @AgentSkill(name = "EchoAgent", executor = EchoAgent.class, canceller = EchoAgentCanceller.class)
 * }))
 * public abstract class MyA2AServer extends LatticeApplication { ... }
 * }</pre>
 */
@Retention(RUNTIME)
public @interface A2AServer {
  Agent agent();
}
