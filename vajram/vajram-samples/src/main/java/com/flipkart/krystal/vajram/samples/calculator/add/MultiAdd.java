package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.traits.UseForPredicateDispatch;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.google.auto.value.AutoAnnotation;
import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import lombok.experimental.UtilityClass;

/** Adds all the {@code numbers} and returns the result */
@Trait
@CallGraphDelegationMode(SYNC)
@InvocableOutsideGraph
public interface MultiAdd extends TraitDef<Integer> {
  @SuppressWarnings("initialization.field.uninitialized")
  interface _Inputs {
    @UseForPredicateDispatch
    @IfAbsent(FAIL)
    List<Integer> numbers();
  }

  @Qualifier
  @Retention(RUNTIME)
  @Target({FIELD, METHOD})
  @interface AdditionMethod {

    MultiAddType value();

    @UtilityClass
    final class Creator {
      public static @AutoAnnotation AdditionMethod create(MultiAddType value) {
        return new AutoAnnotation_MultiAdd_AdditionMethod_Creator_create(value);
      }
    }
  }

  enum MultiAddType {
    SIMPLE,
    CHAIN,
    SPLIT,
  }
}
