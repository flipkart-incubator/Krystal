package com.flipkart.krystal.vajram.annos;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.core.KrystalElement.VajramRoot;
import com.flipkart.krystal.vajram.ComputeDelegationMode;
import java.lang.annotation.Documented;
import java.lang.annotation.Target;

/**
 * In Vajram code, app devs must not use this explicitly - it is auto-inferred by the platform.
 *
 * <p>On a Vajram Trait, this is used to specify what "maximum" delegation that can be performed by
 * the vajrams which conform this trait. For example, if a trait has {@code
 * CallGraphDelegationMode}({@link ComputeDelegationMode#SYNC SYNC_DELEGATION}), then conformant
 * vajrams can either have their CallGraphDelegationMode as {@link ComputeDelegationMode#NONE
 * NO_DELEGATION} or {@link ComputeDelegationMode#SYNC SYNC_DELEGATION}. If a vajram trait doesn't
 * have this annotation, then it defaults to {@code CallGraphDelegationMode}({@link
 * ComputeDelegationMode#SYNC SYNC_DELEGATION})
 *
 * <p>For Vajrams, this annotation is auto-inferred from two things, the @{@link
 * OutputLogicDelegationMode} of this vajram, and the CallGraphDelegationMode of the dependencies of
 * this vajram.
 *
 * <p>Since bumping up the CallGraphDelegationMode from {@link ComputeDelegationMode#NONE
 * NO_DELEGATION} to {@link ComputeDelegationMode#SYNC SYNC_DELEGATION} can be backward incompatible
 * especially from the perspective of code which invokes vajrams from outside the Krystal Graph,
 * Vajrams which have the @{@link ExternalInvocation#allow}{@code == true} MUST explicitly specify
 * their CallGraphDelegationMode as part of their contractual obligation to invokers of the Krystal
 * Graph. Tools can catch vialations in backward compatibility in such cases.
 *
 * @see ComputeDelegationMode
 */
@Documented
@Transitive
@ApplicableToElements(VajramRoot.class)
@Target(TYPE)
public @interface CallGraphDelegationMode {
  ComputeDelegationMode value();
}
