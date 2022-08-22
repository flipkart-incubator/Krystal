package com.flipkart.krystal.vajram.inputs;

import com.google.errorprone.annotations.CheckReturnValue;

public record InputCommand(Command command, String reason) {
  public enum Command {
    SKIP,
    EXECUTE,
  }

  @CheckReturnValue
  public static InputCommand skip(String reason){
    return new InputCommand(Command.SKIP, reason);
  }

  @CheckReturnValue
  public static InputCommand execute(){
    return new InputCommand(Command.EXECUTE, "Continue with execution");
  }
}
