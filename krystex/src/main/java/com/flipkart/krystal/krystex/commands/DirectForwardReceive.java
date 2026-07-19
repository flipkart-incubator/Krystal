package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.krystex.kryon.DirectResponse;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import java.util.List;

public sealed interface DirectForwardReceive
    extends MultiRequestDirectCommand, ServerSideCommand<DirectResponse>
    permits DirectForwardCommand {
  List<ExecutionItem> executionItems(KryonDefinitionRegistry kryonDefinitionRegistry);
}
