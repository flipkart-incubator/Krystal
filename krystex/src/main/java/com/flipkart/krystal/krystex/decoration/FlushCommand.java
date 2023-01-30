package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.krystex.node.DependantChain;

public record FlushCommand(DependantChain dependantsChain) implements LogicDecoratorCommand {}
