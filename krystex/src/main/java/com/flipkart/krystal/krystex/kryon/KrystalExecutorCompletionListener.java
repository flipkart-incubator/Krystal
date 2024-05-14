package com.flipkart.krystal.krystex.kryon;

/** Default implementation of Krystal executor which */
public interface KrystalExecutorCompletionListener {

  public default void onComplete() {
    // do nothing
  }
}
