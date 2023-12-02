package com.flipkart.krystal.delayablevajram.samples.uservalidation;

import com.flipkart.krystal.delayablevajram.samples.uservalidation.ValidateUserDocumentsInputUtil.ValidateUserDocumentsInputs;
import com.flipkart.krystal.futures.DelayableFuture;
import com.flipkart.krystal.honeycomb.CallbackInfo;
import com.flipkart.krystal.honeycomb.DelayableEnv;
import com.flipkart.krystal.vajram.DelayableVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import jakarta.inject.Inject;

@SuppressWarnings("initialization.field.uninitialized")
@VajramDef
public abstract class ValidateUserDocuments
    extends DelayableVajram<ValidationTaskCreated, ValidationResponse> {

  @Input String userId;
  @Input String idProofLink;
  @Input String addressProofLink;

  @Inject DelayableEnv delayableEnv;
  @Inject DocValidationServiceClient serviceClient;

  @VajramLogic
  static DelayableFuture<ValidationTaskCreated, ValidationResponse>
      sendDocumentsForDetailedValidation(ValidateUserDocumentsInputs facets) {
    DelayableEnv delayableEnv = facets.delayableEnv();
    CallbackInfo callBackInfo = delayableEnv.getCallBackInfo();
    DocValidationServiceClient serviceClient = facets.serviceClient();
    return delayableEnv.awaitDelayableResponse(
        serviceClient.validateIdAndAddressProofs(
            facets.userId(),
            facets.idProofLink(),
            facets.addressProofLink(),
            callBackInfo.callbackEndpoint(),
            delayableEnv.stringSerializer().apply(callBackInfo.callbackPayload())));
  }
}
