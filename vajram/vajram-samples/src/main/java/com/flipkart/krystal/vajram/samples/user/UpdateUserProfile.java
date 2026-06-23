package com.flipkart.krystal.vajram.samples.user;

import static com.flipkart.krystal.data.DataAccess.AccessPattern.MUTATION;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.data.DataAccess;
import com.flipkart.krystal.krystex.caching.RequestLevelCacheInvalidator;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.samples.user.response_pojos.UserProfile;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Vajram
@DataAccess(datasetName = "UserProfile", accessPattern = MUTATION)
@DataAccess(datasetName = "User")
public abstract class UpdateUserProfile extends IOVajramDef<Boolean> {

  interface _Inputs {
    @IfAbsent(FAIL)
    String userProfileId();

    @IfAbsent(FAIL)
    UserProfile userProfile();
  }

  public static LongAdder CALL_COUNTER = new LongAdder();

  interface _InternalFacets {
    @Inject
    @IfAbsent(FAIL)
    RequestLevelCacheInvalidator reqCacheInvalidator();

    @Inject
    @Named("UpdateUserProfile.shouldUpdate")
    boolean shouldUpdate();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  @Output
  static CompletableFuture<Boolean> updateProfile(
      String userProfileId,
      UserProfile userProfile,
      RequestLevelCacheInvalidator reqCacheInvalidator,
      Optional<Boolean> shouldUpdate) {
    CALL_COUNTER.increment();
    if (!shouldUpdate.orElse(true)) {
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
        getUserProfileReq -> userProfileId.equals(getUserProfileReq.userProfileId()));
  }
}
