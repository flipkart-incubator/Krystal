package com.flipkart.krystal.vajram.samples.greeting;

import static com.flipkart.krystal.vajram.samples.greeting.GreetingRequest.userId_n;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.samples.greeting.GreetingFacetUtil.GreetingFacets;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Optional;

/**
 * Given a userId, this Vajram composes and returns a 'Hello!' greeting addressing the user by name
 * (as declared by the user in their profile).
 */
@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
// ComputeVajram means that this Vajram does not directly perform any blocking operations.
public abstract class Greeting extends ComputeVajram<String> {

  @Input String userId;

  @Inject Optional<Logger> log;

  @Inject
  @Named("analytics_sink")
  AnalyticsEventSink analyticsEventSink;

  @Dependency(onVajram = UserService.class)
  Optional<UserInfo> userInfo;

  // Resolving (or providing) inputs of dependencies
  // is the responsibility of this Vajram (inputs of a vajram are resolved by its client Vajrams).
  // In this case the UserServiceVajram needs a user_id to retrieve user info from User Service.
  // So it's GreetingVajram's responsibility to provide that input.
  @Resolve(depName = "userInfo", depInputs = "userId")
  public static String userIdForUserService(@Using(userId_n) String userId) {
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
  @Output
  public static String createGreetingMessage(GreetingFacets facets) {
    String userId = facets.userId();
    Optional<UserInfo> userInfo = facets.userInfo();
    String greeting =
        "Hello " + userInfo.map(UserInfo::userName).orElse("friend") + "! Hope you are doing well!";
    facets.log().ifPresent(l -> l.log(Level.INFO, greeting));
    facets.log().ifPresent(l -> l.log(Level.INFO, "Greeting user " + userId));
    facets.analyticsEventSink().pushEvent("event_type", new GreetingEvent(userId, greeting));
    return greeting;
  }
}
