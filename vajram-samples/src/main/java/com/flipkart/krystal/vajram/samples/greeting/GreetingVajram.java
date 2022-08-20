package com.flipkart.krystal.vajram.samples.greeting;

import static com.flipkart.krystal.datatypes.JavaType.java;
import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.samples.greeting.GreetingVajram.ID;

import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.BindFrom;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.ResolutionSources;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.VajramDependencyDefinition;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.List;

/**
 * Given a userId, this Vajram composes and returns a 'Hello!' greeting addressing the user by name
 * (as declared by the user in their profile).
 */
@VajramDef(ID) // Unique Id of this Vajram
// SyncVajram means that this Vajram does not directly perform any blocking operations.
public class GreetingVajram extends NonBlockingVajram {
  public static final String ID = "com.flipkart.greetingVajram";

  // Static declaration of all the inputs of this Vajram.
  // This includes inputs provided by clients of this vajram,
  // design-time dependencies of this vajram, as well as
  // objects like loggers and metrics collectors injected by the runtime.
  @Override
  public List<VajramDependencyDefinition> getInputDefinitions() {
    return Arrays.asList(
        Input.builder()
            // Local name for this input
            .name("user_id")
            // Data type - used for code generation
            .type(string())
            // If this input is not provided by the client, throw a build time error.
            .isMandatory()
            .build(),
        Dependency.builder()
            // Data type of resolved dependencies is inferred from the
            // dependency Vajram's Definition
            .name("user_info_fetch")
            // GreetingVajram needs UserService's Response to compose the Greeting
            // which it can get from the UserServiceVajram
            // (which is an Async Vajram as it makes network calls.
            .dependencySpec(vajramID(UserServiceVajram.ID))
            // If this dependency fails, fail this Vajram
            .isMandatory()
            .build(),
        Input.builder()
            .name("log")
            .type(java(Logger.class))
            // This is not expected from clients
            // This is expected to be injected by the runtime
            .resolvableBy(ResolutionSources.SESSION)
            .build(),
        Input.builder()
            // Data type of resolved dependencies is inferred from the
            // dependency Vajram's Definition
            .name("push_analytics_event")
            .type(java(AnalyticsEventSink.class))
            .resolvableBy(ResolutionSources.SESSION)
            .build());
  }

  // Resolving (or providing) unresolved inputs of resolved dependencies
  // is the responsibility of
  // this Vajram (unresolved inputs are resolved by client Vajrams).
  // In this case the UserServiceVajram needs a user Id to retrieve user info from User Service.
  // So it's GreetingVajram's responsibility to provide that input.
  @Resolve(value = "user_info_fetch", inputs = UserServiceVajram.USER_ID)
  public String userIdForUserService(@BindFrom("user_id") String userId) {
    return userId;
  }

  // This is the core business logic of this Vajram
  // Sync vajrams can return any object. AsyncVajrams need to return {CompletableFuture}s
  @VajramLogic
  public String call(String userId, UserInfo userInfo, Logger log, FdpEventSink fdpEventSink) {
    String greeting = "Hello " + userInfo.name() + "! Hope you are doing well!";
    log.log(Level.INFO, "Greeting user " + userId);
    fdpEventSink.pushEvent("event_type", new GreetingEvent(userId, greeting));
    return greeting;
  }
}
