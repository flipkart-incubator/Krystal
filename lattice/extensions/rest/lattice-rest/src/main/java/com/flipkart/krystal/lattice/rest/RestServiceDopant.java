package com.flipkart.krystal.lattice.rest;

import static com.flipkart.krystal.lattice.core.headers.StandardHeaderNames.REQUEST_ID;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Unit;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.lattice.core.di.Bindings;
import com.flipkart.krystal.lattice.core.di.Bindings.BindingsBuilder;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.DopantWithAnnotation;
import com.flipkart.krystal.lattice.core.headers.Header;
import com.flipkart.krystal.lattice.core.headers.SingleValueHeader;
import com.flipkart.krystal.lattice.rest.RestServiceSpec.RestServiceSpecBuilder;
import com.flipkart.krystal.lattice.rest.api.status.HttpResponseStatusException;
import com.flipkart.krystal.lattice.vajram.VajramDopant;
import com.flipkart.krystal.lattice.vajram.VajramRequestExecutionContext;
import com.flipkart.krystal.lattice.vajram.VajramRequestExecutionContext.VajramRequestExecutionContextBuilder;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.tags.Names;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jboss.resteasy.core.ResteasyContext;

@Slf4j
@DopantType(RestServiceDopant.REST_SERVICE_DOPANT_TYPE)
public abstract class RestServiceDopant implements DopantWithAnnotation<RestService> {

  static final String REST_SERVICE_DOPANT_TYPE = "krystal.lattice.restService";
  private final VajramDopant vajramDopant;

  @Inject
  protected RestServiceDopant(RestServiceDopantInitData initData) {
    this.vajramDopant = initData.vajramDopant();
  }

  public <RespT> CompletionStage<Response> executeHttpRequest(
      ImmutableRequest<RespT> vajramRequest, HttpHeaders headers, UriInfo uriInfo)
      throws HttpResponseStatusException {
    List<String> requestIds = headers.getRequestHeader(REQUEST_ID);
    KryonExecutorConfigBuilder executorConfigBuilder = KryonExecutorConfig.builder();
    if (requestIds != null && !requestIds.isEmpty()) {
      executorConfigBuilder.executorId(requestIds.get(0));
    }
    return executeHttpRequest(
            vajramRequest, getRequestScopeSeeds(headers, uriInfo), executorConfigBuilder)
        .thenApply(
            response -> {
              ResponseBuilder responseBuilder;
              int contentLength = 0;
              try {
                if (response == null || response instanceof Unit) {
                  responseBuilder = Response.ok();
                } else if (response instanceof byte[] bytes) {
                  contentLength = bytes.length;
                  responseBuilder = Response.ok(bytes);
                } else if (response instanceof SerializableModel serializableResponse) {
                  byte[] bytes = serializableResponse._serialize();
                  contentLength = bytes.length;
                  responseBuilder =
                      Response.ok(bytes)
                          .header(
                              CONTENT_TYPE, serializableResponse._serdeProtocol().contentType());
                } else if (response instanceof ResponseBuilder rb) {
                  return rb.build();
                } else if (response instanceof Response r) {
                  return r;
                } else {
                  log.error(
                      "Executing vajram request of type {} an unsupported response model of type {}. Supported types are: {}",
                      vajramRequest.getClass(),
                      response.getClass(),
                      List.of(
                          byte[].class,
                          SerializableModel.class,
                          ResponseBuilder.class,
                          Response.class));
                  responseBuilder = Response.serverError();
                }
              } catch (Throwable e) {
                responseBuilder = Response.serverError();
              }
              return responseBuilder.header(CONTENT_LENGTH, contentLength).build();
            });
  }

  public <RespT> CompletionStage<RespT> executeHttpRequest(
      ImmutableRequest<RespT> vajramRequest,
      Bindings requestScopeSeeds,
      KryonExecutorConfigBuilder kryonExecutorConfigBuilder)
      throws HttpResponseStatusException {
    try {
      VajramRequestExecutionContextBuilder<RespT> contextBuilder =
          VajramRequestExecutionContext.<RespT>builder()
              .vajramRequest(vajramRequest)
              .requestScopeSeeds(requestScopeSeeds)
              .executorConfigBuilder(kryonExecutorConfigBuilder);
      transferResteasyContext(contextBuilder);
      return vajramDopant.executeRequest(contextBuilder.build());
    } catch (LeaseUnavailableException e) {
      log.error("Could not lease out single thread executor. Aborting request", e);
      throw new HttpResponseStatusException(StatusCodes.LEASE_UNAVAILABLE);
    }
  }

  /**
   * Transfer the RESTEasy context to Krystal thread so that request scope data accesses work as
   * expected.
   */
  private static <RespT> void transferResteasyContext(
      VajramRequestExecutionContextBuilder<RespT> contextBuilder) {

    Map<Class<?>, Object> contextDataMap = ResteasyContext.getContextDataMap(false);
    if (contextDataMap != null) {
      contextBuilder.requestScopeInitializer(
          () -> ResteasyContext.addCloseableContextDataLevel(contextDataMap));
    }
  }

  private static Bindings getRequestScopeSeeds(HttpHeaders headers, UriInfo uriInfo) {
    BindingsBuilder seeds = Bindings.builder();
    seeds.bind(HttpHeaders.class, headers);
    seeds.bind(UriInfo.class, uriInfo);
    for (Entry<String, List<String>> entry : headers.getRequestHeaders().entrySet()) {
      var name = entry.getKey();
      List<String> value = entry.getValue();
      if (value != null) {
        Header header = Header.of(name, value);
        seeds.bind(Header.class, Names.named(name), header);
        if (header instanceof SingleValueHeader singleValueHeader) {
          seeds.bind(SingleValueHeader.class, Names.named(name), singleValueHeader);
        }
      }
    }
    return seeds.build();
  }

  public List<? extends @NonNull Object> getResources() {
    return List.of();
  }

  public static RestServiceSpecBuilder restService() {
    return RestServiceSpec.builder();
  }

  protected record RestServiceDopantInitData(VajramDopant vajramDopant) {

    @Inject
    protected RestServiceDopantInitData {}
  }
}
