package com.flipkart.krystal.vajram.samples.user;

import static com.flipkart.krystal.datatypes.Trilean.FALSE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static java.util.Objects.requireNonNullElse;

import com.flipkart.krystal.data.MutatesState;
import com.flipkart.krystal.datatypes.Trilean;
import com.flipkart.krystal.krystex.caching.RequestLevelCacheInvalidator;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.samples.user.RunUserWorkflow._InternalFacets;
import com.flipkart.krystal.vajram.samples.user.response_pojos.User;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

@Vajram
@MutatesState(FALSE)
public abstract class GetUser extends IOVajramDef<User> {

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    String userId;
  }

  @Output
  static CompletableFuture<User> fetchUser(String userId) {
    CompletableFuture<User> result = new CompletableFuture<>();

    if ("Incorrect_User_Id".equals(userId)) {
      result.completeExceptionally(new RuntimeException("GetUser failed for userId: " + userId));
    } else {
      result.complete(new User("Profile_Id_" + userId, userId));
    }

    return result;
  }
}
