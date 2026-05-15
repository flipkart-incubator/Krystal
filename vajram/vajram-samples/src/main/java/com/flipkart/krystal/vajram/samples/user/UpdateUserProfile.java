package com.flipkart.krystal.vajram.samples.user;

import static com.flipkart.krystal.datatypes.Trilean.TRUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.core.MutatesState;
import com.flipkart.krystal.krystex.caching.RequestLevelCacheConfig;
import com.flipkart.krystal.krystex.caching.RequestLevelCacheInvalidator;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.samples.user.response_pojos.UserProfile;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Vajram
@MutatesState(TRUE)
@RequestLevelCacheConfig(canInvalidateCacheOf = GetUserProfile_Req.class)
public abstract class UpdateUserProfile extends IOVajramDef<Boolean> {

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    String userProfileId;

    @IfAbsent(FAIL)
    UserProfile userProfile;
  }

  @SuppressWarnings("initialization.field.uninitialized")
  static class _InternalFacets {
    @Inject
    @IfAbsent(FAIL)
    RequestLevelCacheInvalidator reqCacheInvalidator;

    @Inject
    @Named("UpdateUserProfile.shouldUpdate")
    @IfAbsent(FAIL)
    boolean shouldUpdate;
  }

  @Output
  static CompletableFuture<Boolean> updateProfile(
      String userProfileId,
      UserProfile userProfile,
      RequestLevelCacheInvalidator reqCacheInvalidator,
      boolean shouldUpdate) {
    if (!shouldUpdate) {
      return CompletableFuture.completedFuture(false);
    }
    invalidateImpactedCacheKeys(userProfileId, reqCacheInvalidator);

    log.info("Updating user profile ID: {}", userProfileId);
    log.info("Updating user profile: {}", userProfile);
    return CompletableFuture.completedFuture(true);
  }

  private static void invalidateImpactedCacheKeys(
      String userProfileId, RequestLevelCacheInvalidator reqCacheInvalidator) {
    reqCacheInvalidator.invalidateCacheKeys(
        GetUserProfile_Req.class,
        getUserProfileReq ->
            userProfileId.equals(((GetUserProfile_Fac) getUserProfileReq).userProfileId()));
  }
}
