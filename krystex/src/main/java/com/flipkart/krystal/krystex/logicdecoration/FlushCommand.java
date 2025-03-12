package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.krystex.kryon.DependentChain;

public record FlushCommand(DependentChain dependantsChain) implements LogicDecoratorCommand {}
