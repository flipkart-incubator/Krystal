package com.flipkart.krystal.krystex.decoration;

public sealed interface DecoratorCommand permits InitiateActiveDepChains, FlushCommand {}
