package com.flipkart.krystal.caramel.model;

import java.util.function.Consumer;
import java.util.function.Function;

public interface TerminatedWorkflow<INPUT, ROOT extends WorkflowPayload, OUTPUT>
    extends WorkflowCompletionStage, Function<INPUT, OUTPUT>, Consumer<INPUT> {}
