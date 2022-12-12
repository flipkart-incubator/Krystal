package com.flipkart.krystal.vajram.exec.test_vajrams.hellofriends;

import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserInfo;
import java.util.Collection;

class HelloFriendsInputUtils {

  record EnrichedRequest(
      HelloFriendsRequest helloFriendsRequest, Collection<TestUserInfo> userInfos) {}
}
