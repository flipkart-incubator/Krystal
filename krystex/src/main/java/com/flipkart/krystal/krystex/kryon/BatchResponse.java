package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.request.RequestId;

record BatchResponse(java.util.Map<RequestId, ValueOrError<Object>> responses)
    implements KryonResponse {}
