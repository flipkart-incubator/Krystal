package com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService;

import static com.flipkart.krystal.model.ModelListBuilder.ofModels;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.model.ModelListBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelListBuilderTest {
  @Test
  void test() {
    ModelListBuilder<
            Proto3LatticeSampleResponse,
            Proto3LatticeSampleResponse_Immut,
            Proto3LatticeSampleResponse_Immut.Builder>
        og = ofModels(List.of());

    List<Proto3LatticeSampleResponse> modelList = og.asImmutModelList();

    assertThat(modelList).size().isZero();

    ModelListBuilder<
            Proto3LatticeSampleResponse,
            Proto3LatticeSampleResponse_Immut,
            Proto3LatticeSampleResponse_Immut.Builder>
        newBuilder = ofModels(modelList);
    newBuilder.addModel(Proto3LatticeSampleResponse_ImmutProto._builder()._build());

    assertThat(og.asImmutModelList()).size().isOne();
  }
}
