package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.google.common.collect.ImmutableList;
import java.util.Set;

class HelloFriendsV2InputUtils {

  record EnrichedRequest(
      HelloFriendsV2Request helloFriendsV2Request,
      Set<String> friendIds,
      ImmutableList<TestUserInfo> userInfo) {}
}
