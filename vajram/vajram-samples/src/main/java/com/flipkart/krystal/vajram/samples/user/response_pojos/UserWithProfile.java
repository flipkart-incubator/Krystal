package com.flipkart.krystal.vajram.samples.user.response_pojos;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

public record UserWithProfile(String userId, String userProfileId, String profileData) {}
