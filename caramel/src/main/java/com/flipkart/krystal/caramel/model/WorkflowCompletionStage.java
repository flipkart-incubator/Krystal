package com.flipkart.krystal.caramel.model;

import java.util.List;
import java.util.function.Consumer;

public interface WorkflowCompletionStage {

  List<WorkflowCompletionStage> getWorkflows();

  interface TerminatedWorkflow<INPUT, ROOT extends WorkflowPayload, OUTPUT>
      extends WorkflowCompletionStage, Consumer<INPUT> {}
}
