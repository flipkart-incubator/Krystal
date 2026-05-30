package com.flipkart.krystal.lattice.ext.rest;

import static com.flipkart.krystal.lattice.ext.rest.RestServiceDopant.REST_SERVICE_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.AutoConfigure;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec.ThreadingStrategySpecBuilder;
import com.flipkart.krystal.lattice.ext.rest.config.RestServiceDopantConfig;
import com.flipkart.krystal.lattice.ext.rest.jakarta.ServletContextEnricher;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;

@Builder(buildMethodName = "_buildSpec")
public record RestServiceDopantSpec(List<ServletContextEnricher> servletContextEnrichers)
    implements DopantSpec<RestService, RestServiceDopantConfig, RestServiceDopant> {

  public RestServiceDopantSpec {
    servletContextEnrichers = ImmutableList.copyOf(servletContextEnrichers);
  }

  @Override
  public Class<RestServiceDopant> dopantClass() {
    return RestServiceDopant.class;
  }

  public void autoConfigure(
      @AutoConfigure ThreadingStrategySpecBuilder threadingStrategyDopantBuilder) {}

  @Override
  public Class<RestServiceDopantConfig> _configurationType() {
    return RestServiceDopantConfig.class;
  }

  @Override
  public String _dopantType() {
    return REST_SERVICE_DOPANT_TYPE;
  }

  public static final class RestServiceDopantSpecBuilder
      implements DopantSpecBuilder<RestService, RestServiceDopantConfig, RestServiceDopantSpec> {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // Used in generated build method
    private final List<ServletContextEnricher> servletContextEnrichers = new ArrayList<>();

    public RestServiceDopantSpecBuilder servletContextEnrichers(
        List<ServletContextEnricher> servletContextEnrichers) {
      this.servletContextEnrichers.addAll(servletContextEnrichers);
      return this;
    }

    public RestServiceDopantSpecBuilder servletContextEnrichers(
        ServletContextEnricher... servletContextEnrichers) {
      this.servletContextEnrichers.addAll(Arrays.asList(servletContextEnrichers));
      return this;
    }

    @Override
    public Class<RestService> _annotationType() {
      return RestService.class;
    }
  }
}
