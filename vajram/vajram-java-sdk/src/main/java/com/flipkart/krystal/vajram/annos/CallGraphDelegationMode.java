package com.flipkart.krystal.vajram.annos;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.vajram.ComputeDelegationMode;
import com.flipkart.krystal.vajram.KrystalElement.Vajram;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * In Vajram code, app devs must not use this explicitly - it is auto-inferred by the platform.
 *
 * <p>On a Vajram Trait, this is used to specify what "maximum" delegation that can be performed by
 * the vajrams which conform this trait.
 *
 * <p>For example, if a trait is annotated with @{@link CallGraphDelegationMode}({@link
 * ComputeDelegationMode#NO_DELEGATION}), then conformant vajrams must also have the same
 * CallGraphDelegationMode. If a trait has @{@link CallGraphDelegationMode}({@link
 * ComputeDelegationMode#SYNC_DELEGATION}),then conformant vajrams can either have their {@link
 * CallGraphDelegationMode} as {@link ComputeDelegationMode#NO_DELEGATION} or {@link
 * ComputeDelegationMode#SYNC_DELEGATION}
 */
@Documented
@Transitive
@ApplicableToElements(Vajram.class)
@Target(TYPE)
public @interface CallGraphDelegationMode {
  ComputeDelegationMode value();
}
