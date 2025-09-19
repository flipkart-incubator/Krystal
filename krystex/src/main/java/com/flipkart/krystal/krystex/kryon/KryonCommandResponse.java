package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.commands.VoidResponse;

public sealed interface KryonCommandResponse permits BatchResponse, VoidResponse, DirectResponse {}
