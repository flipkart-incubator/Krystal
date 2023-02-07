package com.flipkart.krystal.krystex.decoration;

public sealed interface LogicDecoratorCommand permits InitiateActiveDepChains, FlushCommand {}
