package com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.model.list.ModelsListBuilder;
import com.flipkart.krystal.model.list.ModelsListView;
import org.junit.jupiter.api.Test;

class ModelsListBuilderTest {
  @Test
  void test() {

    ModelsListView<Proto2024eLatticeSampleResponse, Proto2024eLatticeSampleResponse_Immut> og =
        ModelsListView.empty();

    var modelListView = og.modelsBuilder().immutModelsView();
    assertThat(modelListView).size().isZero();

    ModelsListBuilder<
            Proto2024eLatticeSampleResponse,
            Proto2024eLatticeSampleResponse_Immut,
            Proto2024eLatticeSampleResponse_Immut.Builder>
        newBuilder = modelListView.modelsBuilder();
    newBuilder.addModel(Proto2024eLatticeSampleResponse_ImmutProto._builder()._build());

    assertThat(og).size().isOne();
    assertThat(modelListView).size().isOne();

    newBuilder.addBuilder(Proto2024eLatticeSampleResponse_ImmutProto._builder());

    assertThat(og).size().isEqualTo(2);
    assertThat(modelListView).size().isEqualTo(2);
  }
}
