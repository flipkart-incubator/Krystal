package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public record BatchResponse(Map<RequestId, Errable<Object>> responses)
    implements KryonResponse {}
