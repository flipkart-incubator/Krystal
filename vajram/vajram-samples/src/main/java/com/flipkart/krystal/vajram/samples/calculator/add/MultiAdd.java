package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.google.auto.value.AutoAnnotation;
import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import lombok.experimental.UtilityClass;

/** Adds all the {@code numbers} and returns the result */
@Trait
@CallGraphDelegationMode(SYNC)
public interface MultiAdd extends TraitDef<Integer> {
  @SuppressWarnings("initialization.field.uninitialized")
  class _Inputs {
    @IfAbsent(FAIL)
    List<Integer> numbers;
  }

  enum MultiAddType {
    SIMPLE,
    CHAIN,
    SPLIT,
  }

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  @interface MultiAddQualifier {
    MultiAddType value();

    @UtilityClass
    final class Creator {
      public static @AutoAnnotation MultiAddQualifier create(MultiAddType value) {
        return new AutoAnnotation_MultiAdd_MultiAddQualifier_Creator_create(value);
      }
    }
  }
}
