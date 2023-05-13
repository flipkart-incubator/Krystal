package com.flipkart.krystal.vajram.samples.greeting;

import static com.flipkart.krystal.vajram.samples.greeting.GreetingRequest.userId_n;
import static com.flipkart.krystal.vajram.samples.greeting.GreetingRequest.userInfo_n;
import static com.flipkart.krystal.vajram.samples.greeting.GreetingVajram.ID;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.Using;
import java.lang.System.Logger.Level;

/**
 * Given a userId, this Vajram composes and returns a 'Hello!' greeting addressing the user by name
 * (as declared by the user in their profile).
 */
@VajramDef(ID) // Unique Id of this Vajram
// SyncVajram means that this Vajram does not directly perform any blocking operations.
public abstract class GreetingVajram extends ComputeVajram<String> {
  public static final String ID = "com.flipkart.greetingVajram";

  // Resolving (or providing) inputs of dependencies
  // is the responsibility of this Vajram (inputs of a vajram are resolved by its client Vajrams).
  // In this case the UserServiceVajram needs a user_id to retrieve user info from User Service.
  // So it's GreetingVajram's responsibility to provide that input.
  @Resolve(
      depName = userInfo_n,
      depInputs = {UserServiceRequest.userId_n})
  public String userIdForUserService(@Using(userId_n) String userId) {
    return userId;
  }

  // TODO: Iterable support for list resolution
  //    @Resolve(value = "user_info", inputs = "UserServiceVajram.AccountId")
  //    public String userIdForUserService1(@BindFrom("user_info") String userId) {
  //        UserServiceRequest userServiceRequest =
  // UserServiceRequest.builder().userId(userId).build();
  //        return userId;
  //    }

  // This is the core business logic of this Vajram
  // Sync vajrams can return any object. AsyncVajrams need to return {CompletableFuture}s
  @VajramLogic
  public String createGreetingMessage(GreetingInputUtil.GreetingAllInputs request) {
    String userId = request.userId();
    ValueOrError<UserInfo> userInfo =
        request.userInfo().get(UserServiceRequest.builder().userId(userId).build());
    String greeting =
        "Hello "
            + userInfo.value().map(UserInfo::userName).orElse("friend")
            + "! Hope you are doing well!";
    request.log().ifPresent(l -> l.log(Level.INFO, "Greeting user " + userId));
    request
        .analyticsEventSink()
        .ifPresent(
            analyticsEventSink ->
                analyticsEventSink.pushEvent("event_type", new GreetingEvent(userId, greeting)));
    return greeting;
  }
}
