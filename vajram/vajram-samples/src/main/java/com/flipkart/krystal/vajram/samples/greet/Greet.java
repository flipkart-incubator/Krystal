package com.flipkart.krystal.vajram.samples.greet;

import static com.flipkart.krystal.vajram.samples.greet.Greet_Fac.userInfo_n;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.data.IfNoValue;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Optional;

/**
 * Given a userId, this Vajram composes and returns a 'Hello!' greeting addressing the user by name
 * (as declared by the user in their profile).
 */
@ExternallyInvocable
@Vajram
@SuppressWarnings({"initialization.field.uninitialized", "optional.parameter"})
// ComputeVajram means that this Vajram does not directly perform any blocking operations.
public abstract class Greet extends ComputeVajramDef<String> {
  static class _Facets {
    @IfNoValue
    @Input String userId;
    @Inject Logger log;

    @IfNoValue
    @Inject
    @Named("analytics_sink")
    AnalyticsEventSink analyticsEventSink;

    @Dependency(onVajram = UserService.class)
    UserInfo userInfo;
  }

  // Resolving (or providing) inputs of its dependencies
  // is the responsibility of this Vajram (i.e. inputs of a vajram are resolved by its client
  // Vajrams).
  // In this case the UserServiceVajram needs a user_id to retrieve the user info.
  // So it's GreetingVajram's responsibility to provide that input.
  @Resolve(dep = userInfo_n, depInputs = UserService_Req.userId_n)
  public static String userIdForUserService(String userId) {
    return userId;
  }

  // This is the core business logic of this Vajram
  // Sync vajrams can return any object. AsyncVajrams need to return {CompletableFuture}s
  @Output
  static String createGreetingMessage(
      String userId,
      Optional<UserInfo> userInfo,
      Optional<Logger> log,
      AnalyticsEventSink analyticsEventSink) {
    String greeting =
        "Hello " + userInfo.map(UserInfo::userName).orElse("friend") + "! Hope you are doing well!";
    log.ifPresent(l -> l.log(Level.INFO, greeting));
    log.ifPresent(l -> l.log(Level.INFO, "Greeting user " + userId));
    analyticsEventSink.pushEvent("event_type", new GreetingEvent(userId, greeting));
    return greeting;
  }
}
