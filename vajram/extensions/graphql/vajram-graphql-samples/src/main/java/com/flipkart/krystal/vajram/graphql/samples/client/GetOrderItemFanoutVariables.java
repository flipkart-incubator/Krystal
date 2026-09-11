package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.ModelRoot.ModelType.REQUEST;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.ForGraphQlOpReq;
import com.flipkart.krystal.vajram.json.Json;

@ForGraphQlOpReq(GetOrderItemFanoutOp.class)
@ModelRoot(type = REQUEST)
@SupportedModelProtocol(Json.class)
public interface GetOrderItemFanoutVariables extends Model {
  @IfAbsent(FAIL)
  String id();

  @IfAbsent(FAIL)
  Integer i1Index();

  @IfAbsent(FAIL)
  Integer i2Index();

  @IfAbsent(FAIL)
  Integer f1Offset();

  @IfAbsent(FAIL)
  Integer f2Offset();
}
