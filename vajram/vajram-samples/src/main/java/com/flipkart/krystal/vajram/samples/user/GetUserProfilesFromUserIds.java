package com.flipkart.krystal.vajram.samples.user;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.samples.user.GetUserProfilesFromUserIds_Fac.userProfiles_n;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.samples.user.response_pojos.UserWithProfile;
import java.util.ArrayList;
import java.util.List;

@ExternallyInvocable
@Vajram
public abstract class GetUserProfilesFromUserIds extends ComputeVajramDef<List<UserWithProfile>> {

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    List<String> userIds;
  }

  @SuppressWarnings("initialization.field.uninitialized")
  static class _InternalFacets {
    @Dependency(onVajram = GetUserWithProfile.class, canFanout = true)
    UserWithProfile userProfiles;
  }

  @Resolve(dep = userProfiles_n, depInputs = GetUserWithProfile_Req.userId_n)
  public static FanoutCommand<String> userIdsForFanout(List<String> userIds) {
    return executeFanoutWith(userIds);
  }

  @Output
  static List<UserWithProfile> collectProfiles(
      FanoutDepResponses<GetUserWithProfile_Req, UserWithProfile> userProfiles) {
    List<UserWithProfile> results = new ArrayList<>();

    userProfiles
        .requestResponsePairs()
        .forEach(pair -> pair.response().valueOpt().ifPresent(results::add));

    return results;
  }
}
