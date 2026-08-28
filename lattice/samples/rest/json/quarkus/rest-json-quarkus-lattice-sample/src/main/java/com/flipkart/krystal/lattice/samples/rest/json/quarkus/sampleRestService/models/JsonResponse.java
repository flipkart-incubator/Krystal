package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.WILL_NEVER_FAIL;
import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.serial.DefaultSerdeProtocol;
import com.flipkart.krystal.vajram.json.Json;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelRoot(type = RESPONSE, pure = false)
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Json.class)
@DefaultSerdeProtocol(Json.class)
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

  /**
   * An optional string that may carry a computation error. Demonstrates {@link Errable} field
   * support in JSON models: serializes as the inner value when present, absent when nil/failure.
   */
  Errable<String> errableMessage();

  // --- Errable-container fields: List<Errable<T>>, Errable<List<T>>,
  // Errable<List<Errable<T>>>, Map<K,Errable<T>>, Errable<Map<K,T>>, Errable<Map<K,Errable<T>>>,
  // for T = primitive / Enum model / Model. ---

  Errable<InnerDataV2> errableDataV2();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Errable<Integer>> errableInts();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Errable<Priority>> errablePriorities();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Errable<InnerDataV2>> errableInnerDataList();

  @IfAbsent(WILL_NEVER_FAIL)
  Errable<List<Integer>> errableIntList();

  @IfAbsent(WILL_NEVER_FAIL)
  Errable<List<Priority>> errablePriorityList();

  @IfAbsent(WILL_NEVER_FAIL)
  Errable<List<InnerDataV2>> errableInnerDataListWhole();

  @IfAbsent(WILL_NEVER_FAIL)
  Errable<List<Errable<Integer>>> errableListOfErrableInts();

  @IfAbsent(WILL_NEVER_FAIL)
  Errable<List<Errable<Priority>>> errableListOfErrablePriorities();

  @IfAbsent(WILL_NEVER_FAIL)
  Errable<List<Errable<InnerDataV2>>> errableListOfErrableInnerData();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, Errable<Integer>> errableIntMap();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, Errable<Priority>> errablePriorityMap();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, Errable<InnerDataV2>> errableInnerDataMap();

  @IfAbsent(WILL_NEVER_FAIL)
  Errable<Map<String, Integer>> errableIntMapWhole();

  @IfAbsent(WILL_NEVER_FAIL)
  Errable<Map<String, Priority>> errablePriorityMapWhole();

  @IfAbsent(WILL_NEVER_FAIL)
  Errable<Map<String, InnerDataV2>> errableInnerDataMapWhole();

  @IfAbsent(WILL_NEVER_FAIL)
  Errable<Map<String, Errable<Integer>>> errableMapOfErrableInts();

  @IfAbsent(WILL_NEVER_FAIL)
  Errable<Map<String, Errable<Priority>>> errableMapOfErrablePriorities();

  @IfAbsent(WILL_NEVER_FAIL)
  Errable<Map<String, Errable<InnerDataV2>>> errableMapOfErrableInnerData();
}
