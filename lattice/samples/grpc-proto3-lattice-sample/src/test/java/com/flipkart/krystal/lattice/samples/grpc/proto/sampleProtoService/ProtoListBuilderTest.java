package com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.model.ImmutableModel.Builder;
import com.flipkart.krystal.model.list.ModelsListBuilder;
import org.junit.jupiter.api.Test;

class ProtoListBuilderTest {

  @Test
  void protoBuilderListModification_success() {
    SubMessage_ImmutProto3.Builder subMessageBuilder = SubMessage_ImmutProto3._builder();
    ModelsListBuilder<ProtoMessage, ProtoMessage_Immut, Builder> protoMessagesBuilder =
        subMessageBuilder.protoMessages().modelsBuilder();
    protoMessagesBuilder.addBuilder(ProtoMessage_ImmutProto3._builder().count(1));
    protoMessagesBuilder.addModel(ProtoMessage_ImmutProto3._builder().count(2)._build());

    ModelsListBuilder<ProtoMessage, ProtoMessage_Immut, ProtoMessage_Immut.Builder>
        protoMessagesBuilder2 = subMessageBuilder.protoMessages().modelsBuilder();

    protoMessagesBuilder2.addBuilder(ProtoMessage_ImmutPojo._builder().count(3));
    protoMessagesBuilder2.addModel(ProtoMessage_ImmutPojo._builder().count(4)._build());

    SubMessage_Immut subMessage = subMessageBuilder._build();

    assertThat(subMessage.protoMessages()).hasSize(4);
  }
}
