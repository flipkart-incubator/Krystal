package com.flipkart.krystal.core;

import com.flipkart.krystal.data.ExecutionItem;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * A wrapper class for all the data that is needed by a vajram to execute its complete call graph.
 *
 * @param executionItems The requests to this vajram and their corresponding future placeholders
 * @param graphExecutor The executor service which is used to execute the krystal graph. Note that
 *     this might be an event loop executor, so no blocking operations are to performed in this.
 *     This useful, for example, in IOVajrams when a piece of logic (Output.unbatch, for example)
 *     needs to be executed in the same thread in which the executor service is running since it has
 *     the relevant logging context etc. configured.
 */
public record GraphExecutionData<T>(
    List<ExecutionItem<T>> executionItems,
    CommunicationFacade communicationFacade,
    ExecutorService graphExecutor) {}
