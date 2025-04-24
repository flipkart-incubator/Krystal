package com.flipkart.krystal.annos;

import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.experimental.UtilityClass;

/**
 * Alows vajram developers to indicate whether the vajram has been designed for direct invocation
 * from outside the krystal graph. Invocation from outside the krystal graph means the
 * KrystexVajramExecutor or the KrystalExecutor is used to invoke the vajram directly (without
 * writing a vajram depending on it).
 *
 * <p>This information is useful to auto infer graph metadata like shared batcher batching policy
 * (the shared dependency chains) etc. in which we need to know the set of vajram which are
 * externally invoked, so that the medata can be computed (for example, to use them as starting
 * points of a DFS). This annotation may also be useful to implement special backward compatibility
 * checks and other automated tests and validation which are more relevant to externally invoked
 * vajrams.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExternallyInvocable {

  @UtilityClass
  final class Creator {
    public static @AutoAnnotation ExternallyInvocable create() {
      return new AutoAnnotation_ExternallyInvocable_Creator_create();
    }
  }
}
