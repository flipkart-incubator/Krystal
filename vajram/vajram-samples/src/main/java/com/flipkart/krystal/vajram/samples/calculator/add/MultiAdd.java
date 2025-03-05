package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.vajram.ComputeDelegationMode.SYNC;
import com.flipkart.krystal.vajram.VajramTrait;
import com.flipkart.krystal.vajram.VajramTraitDef;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.google.auto.value.AutoAnnotation;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Adds all the {@code numbers} and returns the result */
@VajramTrait
@CallGraphDelegationMode(SYNC)
public abstract class MultiAdd implements VajramTraitDef<Integer> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {
    @Mandatory @Input List<Integer> numbers;
  }

  public enum MultiAdderType {
    SIMPLE,
    CHAIN,
    SPLIT,
  }

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface MultiAddQualifier {
    MultiAdderType value();

    public final class Creator {
      public static @AutoAnnotation MultiAddQualifier create(MultiAdderType value) {
        return new AutoAnnotation_MultiAdd_MultiAddQualifier_Creator_create(value);
      }

      private Creator() {}
    }
  }
}
