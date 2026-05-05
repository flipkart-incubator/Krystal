package com.flipkart.krystal.lattice.samples.grpc.proto3.sampleProtoService;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.model.list.ModelsListBuilder;
import com.flipkart.krystal.model.list.ModelsListView;
import org.junit.jupiter.api.Test;

class Proto3ModelsListBuilderTest {
  @Test
  void listBuilder_mutation_success() {

    ModelsListView<Proto3LatticeSampleResponse, Proto3LatticeSampleResponse_Immut> og =
        ModelsListView.empty();

    var modelListView = og.modelsBuilder().immutModelsView();
    assertThat(modelListView).size().isZero();

    ModelsListBuilder<
            Proto3LatticeSampleResponse,
            Proto3LatticeSampleResponse_Immut,
            Proto3LatticeSampleResponse_Immut.Builder>
        newBuilder = modelListView.modelsBuilder();
    newBuilder.addModel(Proto3LatticeSampleResponse_ImmutProto3._builder()._build());

    assertThat(og).size().isOne();
    assertThat(modelListView).size().isOne();

    newBuilder.addBuilder(Proto3LatticeSampleResponse_ImmutProto3._builder());

    assertThat(og).size().isEqualTo(2);
    assertThat(modelListView).size().isEqualTo(2);
  }
}
