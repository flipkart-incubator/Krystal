package com.flipkart.krystal.vajram.samples.user;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.samples.user.RunUserWorkflow_Fac.updateProfile_s;
import static com.flipkart.krystal.vajram.samples.user.RunUserWorkflow_Fac.userId_s;
import static com.flipkart.krystal.vajram.samples.user.RunUserWorkflow_Fac.userProfile_n;
import static com.flipkart.krystal.vajram.samples.user.RunUserWorkflow_Fac.userWithProfile_s;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.user.response_pojos.UserProfile;
import com.flipkart.krystal.vajram.samples.user.response_pojos.UserWithProfile;
import com.google.common.collect.ImmutableCollection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExternallyInvocable
@Vajram
public abstract class RunUserWorkflow extends ComputeVajramDef<Boolean> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    String userId;
  }

  @SuppressWarnings("initialization.field.uninitialized")
  static class _InternalFacets {
    @IfAbsent(FAIL)
    @Dependency(onVajram = GetUserWithProfile.class)
    UserWithProfile userWithProfile;

    @IfAbsent(FAIL)
    @Dependency(onVajram = UpdateUserProfile.class)
    boolean updateProfile;

    @IfAbsent(FAIL)
    @Dependency(onVajram = GetUserProfile.class)
    UserProfile userProfile;
  }

  @Override
  public ImmutableCollection<? extends SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            userWithProfile_s,
            depInput(GetUserWithProfile_Req.userId_s).usingAsIs(userId_s).asResolver()),
        dep(
            updateProfile_s,
            depInput(UpdateUserProfile_Req.userProfileId_s)
                .using(userWithProfile_s)
                .asResolver(UserWithProfile::userProfileId),
            depInput(UpdateUserProfile_Req.userProfile_s)
                .using(userWithProfile_s)
                .asResolver(userWithProfile -> new UserProfile(userWithProfile.profileData()))));
  }

  @Resolve(dep = userProfile_n, depInputs = GetUserProfile_Req.userProfileId_n)
  static String userProfileId(UserWithProfile userWithProfile, boolean updateProfile) {
    log.info("UserWithProfile: {}", userWithProfile);
    log.info("UpdateProfile: {}", updateProfile);
    return userWithProfile.userProfileId();
  }

  @Output
  static boolean output(UserProfile userProfile, boolean updateProfile) {
    log.info("UserProfile: {}", userProfile);
    return updateProfile;
  }
}
