package flipkart.krystal.lattice.ext.rest.quarkus;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StatusCodes {
  public static final HttpResponseStatus LEASE_UNAVAILABLE = HttpResponseStatus.TOO_MANY_REQUESTS;
}
