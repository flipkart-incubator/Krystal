package com.flipkart.krystal.vajram.annos;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.vajram.ComputeDelegationMode;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * If a vajram has an annotaion which is marked transitive, the annotation is automatically applied
 * to all client vajrams of this vajram. This is useful in cases where some aspects of a vajram are
 * "contagious" and impact the client vajrams as well. We say the property represented by the
 * annotation is transitive and it is "induced" the client vajrams.
 *
 * <p><b>Conflict resolution:</b>
 *
 * <p>It is possible that a vajram is induced with multiple conficting versions of the same
 * annotation. In these cases, the client vajram is expected to perform a "conflict resolution" or
 * "merge" operation to resolve the conflict. The designers of transitive annotations must design
 * for allowing this. If such a resolution/merge is not possible, the transitive annotation must be
 * marked @{@link Repeatable}, so that all the induced vajrams can be assigned to the client vajram.
 *
 * <p>Examples: When a vajram is marked as a write/mutation vajram which modifies database state,
 * all it clients should automatically be marked as a write/mutation vajram as well without the
 * developer having to explicity set the annotaiton. This allowws us to prevent many bugs and
 * errors. If a vajram depends on both a Read Vajram and a Write Vajram, Write takes precedence
 * since a vajram is a mutation vajram if ANY of its dependency mutate state. Another example of a
 * transitive is annotation is @{@link CallGraphDelegationMode}. In this case as well there is a
 * clear hierarchy of {@link ComputeDelegationMode}s allowing clients to resolve conflicts.
 */
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface Transitive {}
