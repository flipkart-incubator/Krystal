package com.flipkart.krystal.vajram.samples.user.response_pojos;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class User {
  String userProfileId;
  String userId;
}
