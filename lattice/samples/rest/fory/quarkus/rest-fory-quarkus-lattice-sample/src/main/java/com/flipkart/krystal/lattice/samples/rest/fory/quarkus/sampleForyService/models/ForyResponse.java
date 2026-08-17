package com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.WILL_NEVER_FAIL;
import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.fory.Fory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelRoot(type = RESPONSE, pure = false)
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Fory.class)
public interface ForyResponse extends Model {

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable String path();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable String queryName();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable String queryAge();

  String message();

  @IfAbsent(WILL_NEVER_FAIL)
  Optional<Integer> optionalInteger();

  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Integer nullableInteger();

  int mandatoryInt();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  int defaultInt();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Integer> intList();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, String> stringMap();

  @IfAbsent(WILL_NEVER_FAIL)
  ForyInnerData nestedData();

  @IfAbsent(WILL_NEVER_FAIL)
  List<ForyInnerData> nestedDataList();

  @IfAbsent(WILL_NEVER_FAIL)
  Map<String, ForyInnerData> namedInnerData();

  /**
   * An optional string that may carry a computation error. Demonstrates {@link Errable} field
   * support in Fory models: serializes as the inner value when present, as nil when absent.
   */
  Errable<String> errableNote();

  Errable<ForyInnerData> errableInnerData();

  Errable<List<Errable<ForyInnerData>>> nestedDataErrableList();
}
