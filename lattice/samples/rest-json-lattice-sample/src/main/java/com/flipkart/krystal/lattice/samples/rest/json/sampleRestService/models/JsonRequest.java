package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.models;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.MAY_FAIL_CONDITIONALLY;
import static com.flipkart.krystal.model.ModelRoot.ModelType.REQUEST;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.json.Json;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@SupportedModelProtocols(Json.class)
@ModelRoot(type = REQUEST)
public interface JsonRequest extends Model {

  @Nullable Integer optionalInput();

  @IfAbsent(FAIL)
  int mandatoryInput();

  @IfAbsent(value = MAY_FAIL_CONDITIONALLY, conditionalFailureInfo = "In some scenarios")
  @Nullable Integer conditionallyMandatoryInput();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  int inputWithDefaultValue();

  @Nullable Long optionalLongInput();

  @IfAbsent(FAIL)
  long mandatoryLongInput();

  @Nullable ByteArray optionalByteString();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Integer> repeatedInts();

  @IfAbsent(FAIL)
  ByteArray defaultByteString();
}
