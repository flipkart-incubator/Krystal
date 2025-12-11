package com.flipkart.krystal.lattice.core.di;

import com.flipkart.krystal.data.Errable;

public interface InjectionValueProvider {
  <T> Errable<T> getInstance(Class<T> clazz);
}
