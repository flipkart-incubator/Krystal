package com.flipkart.krystal.lattice.rest;

import static com.flipkart.krystal.lattice.rest.RestServiceDopant.REST_SERVICE_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilderWithAnnotation;
import lombok.Builder;
import org.checkerframework.checker.nullness.qual.Nullable;

@Builder
record RestServiceSpec() implements DopantSpec<RestService, NoConfiguration, RestServiceDopant> {

  @Override
  public Class<RestServiceDopant> dopantClass() {
    return RestServiceDopant.class;
  }

  public static class RestServiceSpecBuilder
      extends DopantSpecBuilderWithAnnotation<RestService, RestServiceSpec> {

    @Override
    public RestServiceSpec _buildSpec(@Nullable RestService annotation) {
      return new RestServiceSpec();
    }

    @Override
    public Class<RestService> _annotationType() {
      return RestService.class;
    }

    @Override
    public String _dopantType() {
      return REST_SERVICE_DOPANT_TYPE;
    }
  }
}
