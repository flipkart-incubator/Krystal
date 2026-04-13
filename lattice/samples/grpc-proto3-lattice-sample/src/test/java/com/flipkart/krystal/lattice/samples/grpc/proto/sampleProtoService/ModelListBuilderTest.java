package com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.model.ModelListBuilder;
import com.flipkart.krystal.model.ModelsListView;
import org.junit.jupiter.api.Test;

class ModelListBuilderTest {
  @Test
  void test() {

    ModelsListView<Proto3LatticeSampleResponse, Proto3LatticeSampleResponse_Immut> og =
        ModelsListView.empty();

    var modelListView = og.modelsBuilder().immutModelsView();
    assertThat(modelListView).size().isZero();

    ModelListBuilder<
            Proto3LatticeSampleResponse,
            Proto3LatticeSampleResponse_Immut,
            Proto3LatticeSampleResponse_Immut.Builder>
        newBuilder = modelListView.modelsBuilder();
    newBuilder.addModel(Proto3LatticeSampleResponse_ImmutProto._builder()._build());

    assertThat(og).size().isOne();
    assertThat(modelListView).size().isOne();

    newBuilder.addBuilder(Proto3LatticeSampleResponse_ImmutProto._builder());

    assertThat(og).size().isEqualTo(2);
    assertThat(modelListView).size().isEqualTo(2);
  }
}
