package com.flipkart.krystal.data;

import static com.flipkart.krystal.data.Errable.nil;

import com.flipkart.krystal.data.One2OneDepResponse.NoRequest;

public sealed interface One2OneDepResponse<R extends Request<T>, T> extends DepResponse<R, T>
    permits NoRequest, RequestResponse {

  Errable<T> response();

  public static <R extends Request<T>, T> One2OneDepResponse<R, T> noRequest() {
    return NoRequest.NO_REQUEST;
  }

  @SuppressWarnings("Singleton")
  static final class NoRequest implements One2OneDepResponse {

    private static final NoRequest NO_REQUEST = new NoRequest();

    private NoRequest() {}

    @Override
    public Errable<Object> response() {
      return nil();
    }
  }
}
