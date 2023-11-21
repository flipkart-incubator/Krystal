package com.flipkart.krystal.krystex.logicdecoration;

public interface DecorableLogic<T, R> {
  R execute(T t);
}
