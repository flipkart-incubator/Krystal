package com.flipkart.krystal.lattice.rest.api.status;

import lombok.Getter;

public final class HttpResponseStatus {

  @Getter private final int statusCode;
  @Getter private final String reasonPhrase;

  /** 404 Not Found */
  public static final HttpResponseStatus NOT_FOUND = newStatus(404, "Not Found");

  /** 429 Too Many Requests (RFC6585) */
  public static final HttpResponseStatus TOO_MANY_REQUESTS = newStatus(429, "Too Many Requests");

  private static HttpResponseStatus newStatus(int statusCode, String reasonPhrase) {
    return new HttpResponseStatus(statusCode, reasonPhrase);
  }

  private HttpResponseStatus(int statusCode, String reasonPhrase) {
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase;
  }
}
