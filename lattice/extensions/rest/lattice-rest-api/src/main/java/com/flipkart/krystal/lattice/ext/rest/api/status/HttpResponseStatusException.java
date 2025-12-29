package com.flipkart.krystal.lattice.ext.rest.api.status;

import com.flipkart.krystal.except.KrystalException;
import lombok.Getter;

public class HttpResponseStatusException extends KrystalException {

  @Getter private final HttpResponseStatus status;

  public HttpResponseStatusException(HttpResponseStatus status) {
    super(status.reasonPhrase());
    this.status = status;
  }
}
