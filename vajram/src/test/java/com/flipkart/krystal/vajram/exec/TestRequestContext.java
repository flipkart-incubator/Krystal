package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.ApplicationRequestContext;
import java.util.Optional;
import lombok.Builder;

@Builder
public record TestRequestContext(Optional<String> loggedInUserId, int numberOfFriends)
    implements ApplicationRequestContext {}
