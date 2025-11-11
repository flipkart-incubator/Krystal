package com.flipkart.krystal.vajram.samples.user;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.samples.user.response_pojos.UserProfile;
import java.util.concurrent.CompletableFuture;

@Vajram
public abstract class GetUserProfile extends IOVajramDef<UserProfile> {

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    String userProfileId;
  }

  @Output
  static CompletableFuture<UserProfile> fetchProfile(String userProfileId) {
    return CompletableFuture.completedFuture(new UserProfile("Profile data for " + userProfileId));
  }
}
