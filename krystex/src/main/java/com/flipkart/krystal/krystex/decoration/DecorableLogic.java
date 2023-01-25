package com.flipkart.krystal.krystex.decoration;

public interface DecorableLogic<T, R> {
  R execute(T t);
}
