package com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService;

import static com.flipkart.krystal.model.ModelListBuilder.ofModels;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.model.ModelListBuilder;
import com.flipkart.krystal.model.ModelsListView;
import org.junit.jupiter.api.Test;

class ModelListBuilderTest {
  @Test
  void test() {

    var modelListView =
        ModelListBuilder
            .<Proto3LatticeSampleResponse, Proto3LatticeSampleResponse_Immut,
                Proto3LatticeSampleResponse_Immut.Builder>
                ofModels(new ModelsListView<>())
            .modelsListView();

    assertThat(modelListView).size().isZero();

    ModelListBuilder<
            Proto3LatticeSampleResponse,
            Proto3LatticeSampleResponse_Immut,
            Proto3LatticeSampleResponse_Immut.Builder>
        newBuilder = ofModels(modelListView);
    newBuilder.addModel(Proto3LatticeSampleResponse_ImmutProto._builder()._build());

    assertThat(modelListView).size().isOne();
  }
}
