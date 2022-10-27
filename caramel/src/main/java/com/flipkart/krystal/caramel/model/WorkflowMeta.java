package com.flipkart.krystal.caramel.model;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;

public class WorkflowMeta<T extends WorkflowPayload> {
  private String version;
  private String name;
  private final Class<T> payloadType;

  public WorkflowMeta(String name, Class<T> payloadType) {
    this.name = name;
    this.payloadType = payloadType;
  }

  public WorkflowMeta<T> workflowName(String name) {
    if (this.name != null) {
      throw new IllegalArgumentException("Cannot assign name twice");
    }
    this.name = name;
    return this;
  }

  @NonNull
  public static <P, T extends WorkflowPayload> WorkflowBuildStage<P, T> workflow(
      InputChannel<P> source, BiConsumer<T, P> consumer) {
    return null;
  }

  public static WorkflowMeta<?> workflow(String name) {
    return new WorkflowMeta<>(name, null);
  }

  public static <P extends WorkflowPayload> WorkflowMeta<P> workflow(
      String name, Class<P> payloadType) {
    return new WorkflowMeta<>(name, payloadType);
  }

  public WorkflowMeta<T> version(String version) {
    this.version = version;
    return this;
  }

  public <P, X extends WorkflowPayload> WorkflowBuildStage<P, X> from(
      InputChannel<P> source, BiConsumer<X, P> contextInitializer) {
    return null;
  }

  public <P> WorkflowBuildStage<P, T> take(Field<P, T> field, Supplier<P> source) {
    return null;
  }
}
