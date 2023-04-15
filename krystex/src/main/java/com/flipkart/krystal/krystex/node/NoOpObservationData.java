package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;import com.flipkart.krystal.data.Results;import com.google.common.collect.ImmutableList;public final class NoOpObservationData implements NodeExecutionReport {@Override public void reportMainLogicStart(NodeId nodeId, NodeLogicId nodeLogicId, ImmutableList<Inputs> inputs) {

  }@Override public void reportMainLogicEnd(NodeId nodeId, NodeLogicId nodeLogicId, Results<Object> results) {

  }}
