package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.logic;

import com.flipkart.krystal.lattice.ext.rest.RestServiceDopant;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("TestPost")
public class TestPost {

  private final RestServiceDopant _restServiceDopant;

  @Inject
  public TestPost(RestServiceDopant _restServiceDopant) {
    this._restServiceDopant = _restServiceDopant;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes
  public void restLatticeSample_Json(
      @Suspended AsyncResponse _asyncResponse,
      @Context HttpHeaders _httpHeaders,
      @Context UriInfo _uriInfo,
      CompletableFuture<byte[]> _body) {
    _body.thenAccept(
        _bytes -> {
          var _vajramRequest = new RestLatticeSample_ReqImmutJson(_bytes);
          this._restServiceDopant
              .executeHttpRequest(_vajramRequest._build(), _httpHeaders, _uriInfo)
              .whenComplete(
                  (_result, _error) -> {
                    if (_error != null) {
                      _asyncResponse.resume(_error);
                    } else {
                      _asyncResponse.resume(_result);
                    }
                  });
        });
  }
}
