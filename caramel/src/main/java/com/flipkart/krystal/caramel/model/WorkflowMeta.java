package com.flipkart.krystal.caramel.model;

public class WorkflowMeta<P extends WorkflowPayload> {
  private String version;
  private String name;
  private final Class<P> payloadType;

  public WorkflowMeta(String name, Class<P> payloadType) {
    this.name = name;
    this.payloadType = payloadType;
  }

  /**
   * Creates a new workflow definition with the given name and payload type. If a workflow with the
   * same already exists, returns the same workflow.
   *
   * @param name The name of the workflow.
   * @param payloadType the payload type of the workflow
   * @return
   * @param <P>
   */
  public static <P extends WorkflowPayload> WorkflowMeta<P> workflow(
      String name, Class<P> payloadType) {
    return new WorkflowMeta<>(name, payloadType);
  }

  public WorkflowMeta<P> version(String version) {
    this.version = version;
    return this;
  }

  /**
   * This defines the starting point of a workflow. Every workflow has exactly one starting point
   * from where the execution of the workflow begins.
   *
   * @param <F> the type of the received message
   * @param field the field of the workflow payload into which the incoming message is collected.
   */
  public <F> WorkflowBuildStage<F, P> startWith(Field<F, P> field) {
    throw new UnsupportedOperationException();
  }
}
