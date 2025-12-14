package com.flipkart.krystal.lattice.ext.quarkus.app;

import static com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationDopant.APPLICATION_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.AutoConfigure;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec.ThreadingStrategySpecBuilder;
import io.smallrye.context.SmallRyeContextManagerProvider;
import lombok.Builder;

@Builder(buildMethodName = "_buildSpec")
public record QuarkusApplicationSpec() implements SimpleDopantSpec<QuarkusApplicationDopant> {

  public void autoConfigure(
      @AutoConfigure ThreadingStrategySpecBuilder threadingStrategySpecBuilder) {
    threadingStrategySpecBuilder.executorServiceTransformer(
        executorService ->
            SmallRyeContextManagerProvider.getManager()
                .newManagedExecutorBuilder()
                .withExecutorService(executorService)
                .build());
  }

  @Override
  public Class<? extends QuarkusApplicationDopant> dopantClass() {
    return QuarkusApplicationDopant.class;
  }

  @Override
  public String _dopantType() {
    return APPLICATION_DOPANT_TYPE;
  }

  public static final class QuarkusApplicationSpecBuilder
      extends SimpleDopantSpecBuilder<QuarkusApplicationSpec> {}
}
