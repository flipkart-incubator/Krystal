package com.flipkart.krystal.vajram.samples.greeting;

import static com.flipkart.krystal.datatypes.StringType.string;

import com.flipkart.krystal.vajram.DefaultModulatedBlockingVajram;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class UserServiceVajram extends DefaultModulatedBlockingVajram<UserInfo> {

  public static final String ID = "userServiceVajram";
  public static final String USER_ID = "user_id";

  @Override
  public List<VajramInputDefinition> getInputDefinitions() {
    return List.of(
        Input.builder()
            // Local name for this input
            .name("user_id")
            // Data type - used for code generation
            .type(string())
            // If this input is not provided by the client, throw a build time error.
            .mandatory()
            .build());
  }

  @VajramLogic
  public CompletableFuture<UserInfo> callUserService(String userId){
    // Make a call to user service and get user info
    return new CompletableFuture<>();
  }
}
