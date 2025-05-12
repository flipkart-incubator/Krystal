package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.krystex.kryon.DependentChain;

public record FlushCommand(DependentChain dependantsChain) implements DecoratorCommand {}
