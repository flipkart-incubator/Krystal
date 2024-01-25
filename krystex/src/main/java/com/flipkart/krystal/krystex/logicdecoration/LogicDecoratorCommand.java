package com.flipkart.krystal.krystex.logicdecoration;

public sealed interface LogicDecoratorCommand permits InitiateActiveDepChains, FlushCommand {}
