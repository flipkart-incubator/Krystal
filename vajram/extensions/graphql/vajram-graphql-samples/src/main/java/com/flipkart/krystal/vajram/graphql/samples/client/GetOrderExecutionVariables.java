package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.ModelRoot.ModelType.REQUEST;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.ForGraphQlOpReq;
import com.flipkart.krystal.vajram.json.Json;

@ForGraphQlOpReq(GetOrderExecutionOp.class)
@ModelRoot(type = REQUEST)
@SupportedModelProtocol(Json.class)
public interface GetOrderExecutionVariables extends Model {
  @IfAbsent(FAIL)
  String orderId();

  @IfAbsent(FAIL)
  String dummyId();

  @IfAbsent(FAIL)
  String userId();
}
