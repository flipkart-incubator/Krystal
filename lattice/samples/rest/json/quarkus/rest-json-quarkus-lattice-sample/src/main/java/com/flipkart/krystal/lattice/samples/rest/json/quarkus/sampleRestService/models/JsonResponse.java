package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.json.Json;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelRoot(type = RESPONSE)
@SupportedModelProtocols({PlainJavaObject.class, Json.class})
public interface JsonResponse extends Model {

  @Nullable String path();

  @Nullable String qp_name();

  @Nullable String qp_age();

  @Nullable String uriInfo();

  String string();

  Optional<Integer> optionalInteger();

  @Nullable Integer nullableIntegerMayFailConditionally();

  @Nullable Integer nullableInteger();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Integer> optionalIntArray();

  int mandatoryInt();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  int defaultInt();

  @Nullable String mandatoryStringPartialConstruction();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, String> mapTypedField();

  @Nullable ByteArray byteArray();

  InnerData nestedData();

  InnerDataV2 nestedDataV2();

  List<InnerData> nestedDataList();

  List<InnerDataV2> nestedDataListV2();
}
