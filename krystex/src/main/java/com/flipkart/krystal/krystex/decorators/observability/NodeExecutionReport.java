package com.flipkart.krystal.krystex.decorators.observability;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableList;

public sealed interface NodeExecutionReport extends ObservationData
    permits DefaultNodeExecutionReport, NoOpObservationData {

  void reportMainLogicStart(NodeId nodeId, NodeLogicId nodeLogicId, ImmutableList<Inputs> inputs);

  void reportMainLogicEnd(NodeId nodeId, NodeLogicId nodeLogicId, Results<Object> results);
}
