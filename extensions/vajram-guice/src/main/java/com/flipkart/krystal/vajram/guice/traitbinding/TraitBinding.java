package com.flipkart.krystal.vajram.guice.traitbinding;

import com.flipkart.krystal.data.Request;
import com.google.inject.Key;

public record TraitBinding<T extends Request<?>, C extends T>(
    Key<T> key, Class<C> concreteVajramRequestType) {}
