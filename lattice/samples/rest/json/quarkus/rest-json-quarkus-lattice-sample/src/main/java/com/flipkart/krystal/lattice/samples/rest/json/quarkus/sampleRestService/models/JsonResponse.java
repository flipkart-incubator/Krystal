package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.WILL_NEVER_FAIL;
import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.vajram.json.Json;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelRoot(type = RESPONSE, pure = false)
@SupportedModelProtocols({PlainJavaObject.class, Json.class})
public interface JsonResponse extends Model {

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable String path();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable String qp_name();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable String qp_age();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable String uriInfo();

  String string();

  @IfAbsent(WILL_NEVER_FAIL)
  Optional<Integer> optionalInteger();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Integer nullableIntegerMayFailConditionally();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Integer nullableInteger();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Integer> optionalIntArray();

  int mandatoryInt();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  int defaultInt();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable String mandatoryStringPartialConstruction();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, String> mapTypedField();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, DataRecord> dataRecords();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable ByteArray byteArray();

  @IfAbsent(WILL_NEVER_FAIL)
  InnerData nestedData();

  @IfAbsent(WILL_NEVER_FAIL)
  InnerDataV2 nestedDataV2();

  @IfAbsent(WILL_NEVER_FAIL)
  List<InnerData> nestedDataList();

  @IfAbsent(WILL_NEVER_FAIL)
  List<InnerDataV2> nestedDataListV2();

  @IfAbsent(WILL_NEVER_FAIL)
  Map<String, InnerData> namedInnerData();

  @IfAbsent(WILL_NEVER_FAIL)
  Map<String, InnerDataV2> namedInnerDataV2();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Priority priority();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Priority optionalPriority();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Priority> priorities();
}
