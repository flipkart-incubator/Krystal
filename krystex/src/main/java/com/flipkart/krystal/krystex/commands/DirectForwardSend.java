package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.krystex.kryon.DirectResponse;
import java.util.List;

public sealed interface DirectForwardSend
    extends MultiRequestDirectCommand, ClientSideCommand<DirectResponse>
    permits DirectForwardCommand {

  List<? extends RequestResponseFuture<? extends Request<?>, ?>> executableRequests();
}
