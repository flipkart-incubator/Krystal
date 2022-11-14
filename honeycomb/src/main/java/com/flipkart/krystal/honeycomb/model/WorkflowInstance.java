package com.flipkart.krystal.honeycomb.model;

import com.flipkart.krystal.honeycomb.UnrecognizedFieldException;

/** A representation of a single execution instance of a workflow */
public interface WorkflowInstance {

  /**
   * Updates the values of the provided fields in the payload store.
   *
   * @param fieldValues the values to be updated
   * @throws UnrecognizedFieldException if any given field name is not part of the workflow payload
   *     definition
   */
  void updateFields(FieldValue... fieldValues) throws UnrecognizedFieldException;

  /**
   * Executes new workflow instances of the provided {@code workflowId} with the provided {@code
   * payloads}. When all the workflows are finished, resumes this workflow instance with their
   * responses
   *
   * @param workflowId The workflow id of the workflow whose instance need to be created and
   *     executed
   * @param payloads the payloads to pass to the forked workflow instances - one each.
   */
  void fork(String workflowId, Object... payloads);
}
