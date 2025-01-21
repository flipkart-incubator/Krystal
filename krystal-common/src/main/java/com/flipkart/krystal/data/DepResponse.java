package com.flipkart.krystal.data;

public sealed interface DepResponse<R extends Request<T>, T> extends FacetValue<T>
    permits FanoutDepResponses, One2OneDepResponse {}
