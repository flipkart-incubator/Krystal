package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2;


import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2.ID;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.BindFrom;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Vajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2InputUtil.MultiHelloFriendsV2AllInputs;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@VajramDef(ID)
public abstract class MultiHelloFriendsV2 extends ComputeVajram<String> {
  public static final String ID = "MultiHelloFriendsV2";

  @Resolve(value = "hellos", inputs = HelloFriendsV2Vajram.USER_ID)
  public Set<String> userIdsForHellos(@BindFrom("user_ids") Set<String> userIds) {
    return userIds;
  }

  @VajramLogic
  public String sayHellos(MultiHelloFriendsV2AllInputs allInputs) {
    LinkedHashSet<String> userIds = allInputs.userIds();
    List<String> result = new ArrayList<>();
    for (String userId : userIds) {
      allInputs
          .hellos()
          .get(HelloFriendsV2Request.builder().userId(userId).build())
          .value()
          .ifPresent(result::add);
    }
    return String.join("\n", result);
  }
}
