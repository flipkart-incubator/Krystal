package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.model.DependantChain;
import com.flipkart.krystal.model.KryonId;

public record Flush(KryonId kryonId, DependantChain dependantChain) implements KryonCommand {}
