package com.flipkart.krystal.lattice.ext.rest;

import static com.flipkart.krystal.lattice.ext.rest.api.status.HttpResponseStatus.TOO_MANY_REQUESTS;

import com.flipkart.krystal.lattice.ext.rest.api.status.HttpResponseStatus;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StatusCodes {
  public static final HttpResponseStatus LEASE_UNAVAILABLE = TOO_MANY_REQUESTS;
}
