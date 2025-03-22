package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;

/** Used when a particular command doesn't expect any repsonse. */
@SuppressWarnings("Singleton")
public final class VoidResponse implements KryonCommandResponse {

  private static final VoidResponse INSTANCE = new VoidResponse();

  public static VoidResponse getInstance() {
    return INSTANCE;
  }

  private VoidResponse() {}
}
