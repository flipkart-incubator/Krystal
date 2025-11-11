package com.flipkart.krystal.vajram.samples.user;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.facets.One2OneCommand.executeWith;
import static com.flipkart.krystal.vajram.samples.user.GetUserWithProfile_Fac.userProfile_n;
import static com.flipkart.krystal.vajram.samples.user.GetUserWithProfile_Fac.user_n;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.samples.user.response_pojos.User;
import com.flipkart.krystal.vajram.samples.user.response_pojos.UserProfile;
import com.flipkart.krystal.vajram.samples.user.response_pojos.UserWithProfile;

@Vajram
public abstract class GetUserWithProfile extends ComputeVajramDef<UserWithProfile> {

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    String userId;
  }

  @SuppressWarnings("initialization.field.uninitialized")
  static class _InternalFacets {
    @IfAbsent(FAIL)
    @Dependency(onVajram = GetUser.class)
    User user;

    @IfAbsent(FAIL)
    @Dependency(onVajram = GetUserProfile.class)
    UserProfile userProfile;
  }

  @Resolve(dep = user_n, depInputs = GetUser_Req.userId_n)
  public static One2OneCommand<String> userIdForGetUser(String userId) {
    return executeWith(userId);
  }

  @Resolve(dep = userProfile_n, depInputs = GetUserProfile_Req.userProfileId_n)
  public static One2OneCommand<String> profileIdForGetUserProfile(User user) {
    return executeWith(user.userProfileId());
  }

  @Output
  static UserWithProfile combine(String userId, User user, UserProfile userProfile) {
    return new UserWithProfile(userId, user.userProfileId(), userProfile.profileData());
  }
}
