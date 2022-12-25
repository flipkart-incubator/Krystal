package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends;

import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.google.common.collect.ImmutableList;

class HelloFriendsInputUtils {

  record EnrichedRequest(
      HelloFriendsRequest helloFriendsRequest,
      TestUserInfo userInfo,
      ImmutableList<TestUserInfo> friendInfos) {}
}
