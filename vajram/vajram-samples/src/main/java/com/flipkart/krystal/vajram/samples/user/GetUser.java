package com.flipkart.krystal.vajram.samples.user;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.samples.user.response_pojos.User;
import java.util.concurrent.CompletableFuture;

@Vajram
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
