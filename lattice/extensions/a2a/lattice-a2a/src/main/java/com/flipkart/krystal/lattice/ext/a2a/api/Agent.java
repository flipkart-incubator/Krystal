package com.flipkart.krystal.lattice.ext.a2a.api;

/**
 * Definition of an A2A agent hosted on an {@link A2AServer}.
 *
 * <p>Each entry in {@link #skills()} defines one agent skill backed by a pair of Vajrams — one for
 * task execution and one for task cancellation.
 *
 * <p>The {@code AgentCard} (describing this server's identity, capabilities and skills) must be
 * provided as a CDI bean by the application; the A2A Java SDK will serve it automatically at {@code
 * /.well-known/agent.json}.
 */
public @interface Agent {

  /**
   * One or more agent skill definitions. Each {@link AgentSkill} maps an agent skill name to the
   * Vajrams that handle task execution and cancellation for that skill.
   */
  AgentSkill[] skills();
}
