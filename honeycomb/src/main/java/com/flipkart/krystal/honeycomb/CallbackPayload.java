package com.flipkart.krystal.honeycomb;

import com.flipkart.krystal.model.DependantChain;
import com.flipkart.krystal.model.KryonId;

public record CallbackPayload(KryonId kryonId, DependantChain dependantChain, String instanceId) {}
