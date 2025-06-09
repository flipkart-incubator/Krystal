package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.MAY_FAIL_CONDITIONALLY;
import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.json.Json;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelRoot(type = RESPONSE)
@SupportedModelProtocols({PlainJavaObject.class, Json.class})
public interface JsonResponse extends Model {
  @IfAbsent(FAIL)
  String string();

  @IfAbsent(MAY_FAIL_CONDITIONALLY)
  Optional<Integer> optionalInteger();

  @IfAbsent(MAY_FAIL_CONDITIONALLY)
  @Nullable Integer nullableIntegerMayFailConditionally();

  @Nullable Integer nullableInteger();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Integer> optionalIntArray();

  @IfAbsent(FAIL)
  int mandatoryInt();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  int defaultInt();

  @IfAbsent(FAIL)
  @Nullable String mandatoryStringPartialConstruction();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  @Nullable Map<String, String> mapTypedField();

  @Nullable ByteArray byteArray();
}
