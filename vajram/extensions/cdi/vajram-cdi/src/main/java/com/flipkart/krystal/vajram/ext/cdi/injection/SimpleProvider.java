package com.flipkart.krystal.vajram.ext.cdi.injection;

import jakarta.inject.Provider;

record SimpleProvider(Provider<?> provider) implements ProviderWrapper {}
