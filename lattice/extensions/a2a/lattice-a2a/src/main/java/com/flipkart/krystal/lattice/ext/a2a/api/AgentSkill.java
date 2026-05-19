package com.flipkart.krystal.lattice.ext.a2a.api;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.vajram.VajramDefRoot;
import java.lang.annotation.Retention;

/**
 * Defines one A2A agent skill within an {@link A2AServer} application.
 *
 * <p>The {@link #executor()} Vajram handles task execution. Its input facets are populated from the
 * incoming {@code RequestContext} using these name conventions:
 *
 * <ul>
 *   <li>{@code userInput} – the plain-text content of the first {@code TextPart} in the incoming
 *       message ({@code RequestContext.getUserInput()})
 *   <li>{@code taskId} – the task identifier ({@code RequestContext.getTaskId()})
 *   <li>{@code contextId} – the conversation context identifier ({@code
 *       RequestContext.getContextId()})
 * </ul>
 *
 * <p>The {@link #canceller()} Vajram, when provided, handles cancellation requests. Its input
 * facets follow the same conventions (typically only {@code taskId} and {@code contextId} are
 * relevant for cancellation). If omitted the runtime simply calls {@code emitter.cancel()}
 * immediately.
 *
 * <p>When multiple {@link AgentSkill} entries are declared on the same {@link Agent}, the runtime
 * routes each incoming task to the correct skill based on the {@code skillId} key in the request
 * metadata ({@code MessageSendParams.metadata()}). If no routing key is present, the first declared
 * skill is used as the default.
 */
@Retention(RUNTIME)
public @interface AgentSkill {

  /** Unique skill name */
  String name();

  /** The Vajram class whose {@code @Output} logic executes the agent task. */
  Class<? extends VajramDefRoot<?>> executor();

  /**
   * The Vajram class whose {@code @Output} logic handles task cancellation. When omitted the
   * runtime responds with an immediate {@code cancel} without invoking any Vajram.
   */
  Class<? extends VajramDefRoot> canceller() default VajramDefRoot.class;
}
