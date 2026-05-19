package com.flipkart.krystal.lattice.samples.a2a.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.List;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;

/**
 * Produces the {@link AgentCard} CDI bean that the A2A SDK serves at {@code
 * /.well-known/agent.json}.
 *
 * <p>This bean automatically overrides the {@code @DefaultBean} provided by the A2A reference SDK,
 * so no additional configuration is needed.
 */
@ApplicationScoped
public class SampleAgentCardProducer {

  @Produces
  @PublicAgentCard
  public AgentCard agentCard() {
    return AgentCard.builder()
        .name("Lattice A2A Sample Agent")
        .description(
            "A sample A2A agent powered by Krystal Lattice, demonstrating echo and reverse skills")
        .version("1.0.0")
        .capabilities(
            AgentCapabilities.builder()
                .streaming(false)
                .pushNotifications(false)
                .extendedAgentCard(false)
                .build())
        .defaultInputModes(List.of("text/plain"))
        .defaultOutputModes(List.of("text/plain"))
        .supportedInterfaces(List.of(new AgentInterface("HTTP+JSON", "/")))
        .skills(
            List.of(
                AgentSkill.builder()
                    .id("echo")
                    .name("Echo")
                    .description("Echoes the user's input back as the agent response")
                    .tags(List.of("echo", "sample"))
                    .examples(List.of("Hello world"))
                    .build(),
                AgentSkill.builder()
                    .id("reverse")
                    .name("Reverse")
                    .description("Reverses the characters of the user's input text")
                    .tags(List.of("reverse", "sample"))
                    .examples(List.of("Hello world"))
                    .build()))
        .build();
  }
}
