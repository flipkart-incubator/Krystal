package com.flipkart.krystal.krystex.kryon;

public sealed interface KryonResponse permits BatchResponse, GranuleResponse, FlushResponse {}
