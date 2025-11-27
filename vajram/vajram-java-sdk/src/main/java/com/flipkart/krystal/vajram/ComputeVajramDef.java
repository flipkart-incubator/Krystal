package com.flipkart.krystal.vajram;

import java.util.concurrent.ExecutorService;

/**
 * Compute vajrams are vajrams whose output logic can be executed in the calling thread itself . In
 * other words, the output logic does not delegate outside the calling thread.
 *
 * <p>This means that ComputeVajrams cannot make network calls to compute the output value. They
 * cannot submit the computation task to an {@link ExecutorService}, nor can they make any other IO
 * calls like disk look-ups or even calls to other processes to compute the output.
 *
 * @param <T> The type of the output of this vajram.
 */
public abstract non-sealed class ComputeVajramDef<T> implements VajramDef<T> {}
