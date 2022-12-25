package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.vajram.ApplicationRequestContext;
import java.util.Optional;
import lombok.Builder;

@Builder
public record TestRequestContext(Optional<String> loggedInUserId, int numberOfFriends)
    implements ApplicationRequestContext {}
