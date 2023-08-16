package com.flipkart.krystal.krystex.kryon;

sealed interface KryonResponse permits BatchResponse, GranuleResponse, FlushResponse {}
