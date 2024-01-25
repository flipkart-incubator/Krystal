package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.krystex.kryon.DependantChain;

public record FlushCommand(DependantChain dependantsChain) implements LogicDecoratorCommand {}
