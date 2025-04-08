package com.flipkart.krystal.lattice.samples.proto3.models;

import static com.flipkart.krystal.data.IfNoValue.Strategy.FAIL;

import com.flipkart.krystal.data.IfNoValue;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;

@ModelRoot
@SupportedModelProtocols({PlainJavaObject.class, Protobuf3.class})
public interface Proto3LatticeSampleResponse extends Model {
  @SerialId(1)
  @IfNoValue(then = FAIL)
  String string();
}
