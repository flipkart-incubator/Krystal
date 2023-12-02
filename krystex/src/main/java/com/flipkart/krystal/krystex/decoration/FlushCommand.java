package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.model.DependantChain;

public record FlushCommand(DependantChain dependantsChain) implements LogicDecoratorCommand {}
