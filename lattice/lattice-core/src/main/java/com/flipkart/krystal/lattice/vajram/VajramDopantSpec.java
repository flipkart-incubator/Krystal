package com.flipkart.krystal.lattice.vajram;

import static com.flipkart.krystal.lattice.vajram.VajramDopant.DOPANT_TYPE;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public record VajramDopantSpec(
    VajramKryonGraph vajramGraph,
    ImmutableList<KryonExecutorConfigurator> kryonExecutorConfigurators)
    implements SimpleDopantSpec<VajramDopant> {

  @Override
  public Class<VajramDopant> dopantClass() {
    return VajramDopant.class;
  }

  @DopantType(DOPANT_TYPE)
  public static final class VajramDopantSpecBuilder
      extends SimpleDopantSpecBuilder<VajramDopantSpec> {

    private VajramKryonGraphBuilder graphBuilder = VajramKryonGraph.builder();

    private final List<Consumer<VajramKryonGraph>> graphProcessors = new ArrayList<>();
    private final List<KryonExecutorConfigurator> kryonExecutorConfigurators = new ArrayList<>();

    public VajramDopantSpecBuilder graphBuilder(VajramKryonGraphBuilder graphBuilder) {
      this.graphBuilder = graphBuilder;
      return this;
    }

    public VajramDopantSpecBuilder extendGraph(Consumer<VajramKryonGraph> processor) {
      graphProcessors.add(processor);
      return this;
    }

    public VajramDopantSpecBuilder configureKryonExecutor(
        KryonExecutorConfigurator executorConfigurator) {
      kryonExecutorConfigurators.add(executorConfigurator);
      return this;
    }

    @SuppressWarnings("ClassEscapesDefinedScope")
    @Override
    public VajramDopantSpec _buildSpec() {
      VajramKryonGraph graph = graphBuilder.build();
      graphProcessors.forEach(p -> p.accept(graph));
      return new VajramDopantSpec(graph, ImmutableList.copyOf(kryonExecutorConfigurators));
    }

    @Override
    public String _dopantType() {
      return DOPANT_TYPE;
    }
  }
}
