package com.flipkart.krystal.caramel.model;

import java.util.function.Consumer;

public interface OutputChannel<T> extends Consumer<T> {

  static <T> OutputChannel<T> outputChannel(String channelName) {
    throw new UnsupportedOperationException();
  }
}
