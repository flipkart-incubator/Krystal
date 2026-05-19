package com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.ModelRoot.ModelType.REQUEST;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.fory.Fory;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@SupportedModelProtocols({Fory.class, PlainJavaObject.class})
@ModelRoot(type = {REQUEST})
public interface ForyRequest extends Model {

  @Nullable Integer optionalInput();

  @IfAbsent(FAIL)
  int mandatoryInput();

  @Nullable Long optionalLongInput();

  @IfAbsent(FAIL)
  long mandatoryLongInput();

  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Integer> repeatedInts();

  @Nullable ForyInnerData innerData();
}
