package com.flipkart.krystal.delayablevajram.samples.uservalidation;

import java.util.concurrent.CompletableFuture;

public interface DocValidationServiceClient {
  // Non-blocking IO call
  CompletableFuture<ValidationTaskCreated> validateIdAndAddressProofs(
      String userId,
      String idProofLink,
      String addressProofLink,
      String callbackUrl,
      String callbackData);
}
