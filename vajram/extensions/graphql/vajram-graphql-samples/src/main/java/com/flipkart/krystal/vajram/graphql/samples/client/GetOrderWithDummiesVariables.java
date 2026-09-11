package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.ModelRoot.ModelType.REQUEST;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.ForGraphQlOpReq;
import com.flipkart.krystal.vajram.json.Json;

@ForGraphQlOpReq(GetOrderWithDummiesOp.class)
@ModelRoot(type = REQUEST)
@SupportedModelProtocol(Json.class)
public interface GetOrderWithDummiesVariables extends Model {
  @IfAbsent(FAIL)
  String id();

  @IfAbsent(FAIL)
  String d1Name();

  @IfAbsent(FAIL)
  String d2Name();
}
