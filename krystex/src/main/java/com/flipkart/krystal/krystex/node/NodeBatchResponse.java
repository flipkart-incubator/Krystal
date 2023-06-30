package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;

public record NodeBatchResponse(ImmutableMap<RequestId, NodeResponse> responses) {}
