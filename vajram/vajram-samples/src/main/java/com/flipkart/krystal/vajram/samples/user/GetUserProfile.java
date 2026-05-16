package com.flipkart.krystal.vajram.samples.user;

import static com.flipkart.krystal.datatypes.Trilean.FALSE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.data.MutatesState;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.samples.user.response_pojos.UserProfile;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

@Vajram
@MutatesState(FALSE)
public abstract class GetUserProfile extends IOVajramDef<UserProfile> {

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    String userProfileId;
  }

  @Output
  static CompletableFuture<UserProfile> fetchProfile(String userProfileId) {
    CALL_COUNTER.add(1);
    return CompletableFuture.completedFuture(new UserProfile("Profile data for " + userProfileId));
  }
}
