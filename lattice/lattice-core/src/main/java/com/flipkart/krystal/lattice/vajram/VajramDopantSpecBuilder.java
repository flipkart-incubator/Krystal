package com.flipkart.krystal.lattice.vajram;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig.KrystexVajramExecutorConfigBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class VajramGraphSpecBuilder extends SimpleDopantSpecBuilder<VajramGraphSpec> {

  private static final String DOPANT_TYPE = "krystal.vajram.graph";

  private VajramKryonGraphBuilder graphBuilder = VajramKryonGraph.builder();

  private final List<Consumer<VajramKryonGraph>> graphProcessors = new ArrayList<>();
  private final List<Consumer<KrystexVajramExecutorConfigBuilder>> vajramExecConfigProcessors =
      new ArrayList<>();
  private final List<KryonExecutorConfigurator> kryonExecutorConfigurators = new ArrayList<>();

  VajramGraphSpecBuilder() {}

  public VajramGraphSpecBuilder graphBuilder(VajramKryonGraphBuilder graphBuilder) {
    this.graphBuilder = graphBuilder;
    return this;
  }

  public VajramGraphSpecBuilder extendGraph(Consumer<VajramKryonGraph> processor) {
    graphProcessors.add(processor);
    return this;
  }

  public VajramGraphSpecBuilder configureVajramExecutor(
      Consumer<KrystexVajramExecutorConfigBuilder> processor) {
    vajramExecConfigProcessors.add(processor);
    return this;
  }

  public VajramGraphSpecBuilder configureKryonExecutor(KryonExecutorConfigurator manager) {
    kryonExecutorConfigurators.add(manager);
    return this;
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  @Override
  public VajramGraphSpec _buildSpec() {
    VajramKryonGraph graph = graphBuilder.build();
    graphProcessors.forEach(p -> p.accept(graph));
    return new VajramGraphSpec(
        graph,
        ImmutableList.copyOf(vajramExecConfigProcessors),
        ImmutableList.copyOf(kryonExecutorConfigurators));
  }

  @Override
  public String _dopantType() {
    return DOPANT_TYPE;
  }
}
