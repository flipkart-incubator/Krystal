package com.flipkart.krystal.lattice.core.execution;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant.DOPANT_TYPE;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilderWithConfig;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import lombok.Builder;

/**
 * @param threadingStrategy
 * @param executorServiceTransformer a function which transforms the single thread executor in some
 *     way. For example, by wrapping it in a context-transferring managed {@link ExecutorService}
 *     decorator.
 */
@DopantType(DOPANT_TYPE)
@Builder(buildMethodName = "_buildSpec")
public record ThreadingStrategySpec(
    ThreadingStrategy threadingStrategy,
    Function<SingleThreadExecutor, ExecutorService> executorServiceTransformer)
    implements DopantSpec<NoAnnotation, ThreadingStrategyConfig, ThreadingStrategyDopant> {

  @Override
  public Class<? extends ThreadingStrategyDopant> dopantClass() {
    return ThreadingStrategyDopant.class;
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  @Override
  public Class<ThreadingStrategyConfig> _configurationType() {
    return ThreadingStrategyConfig.class;
  }

  @Override
  public String _dopantType() {
    return DOPANT_TYPE;
  }

  public static final class ThreadingStrategySpecBuilder
      extends DopantSpecBuilderWithConfig<ThreadingStrategyConfig, ThreadingStrategySpec> {}
}
