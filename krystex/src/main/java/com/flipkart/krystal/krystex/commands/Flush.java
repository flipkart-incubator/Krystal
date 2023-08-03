package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;

public record Flush(KryonId kryonId, DependantChain dependantChain) implements KryonCommand {}
