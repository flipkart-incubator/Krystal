package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public record NodeBatchResponse(Map<RequestId, NodeResponse> responses) {}
