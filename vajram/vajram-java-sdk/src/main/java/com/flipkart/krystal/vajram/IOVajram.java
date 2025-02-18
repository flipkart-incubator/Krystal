package com.flipkart.krystal.vajram;

import java.util.concurrent.ExecutorService;

/**
 * IO Vajrams are vajrams whose output logics are allowed to delegate outside the calling thread.
 * This means that in the process of computing the output, IO vajrams can communicate with other
 * processes over the network (HTTP API calls, for example), they can communicate processes that are
 * co-hosted with the current process, they can access the local or a remote file system, they can
 * submit output computation tasks to an {@link ExecutorService} etc. as long as the response is
 * expected pretty soon (in the order of seconds).
 *
 * <p>While IO Vajrams are allowed to make such calls, they are not allowed to block the calling
 * thread. All calls that delegate computation outside the current thread must be non-blocking in
 * nature (nonblocking-io, for example). Not adhering this can have serious repurcussions on the
 * performance of the runtime.
 *
 * <p>Note on naming: IO is a special kind of compute delegation. Technically this class represents
 * a "compute delegating vajram". But for reasons of brevity, readability, familiarity etc. and
 * because making network calls is the most common use case for writing such vajrams, this class has
 * been named IOVajram
 *
 * @param <T> The type of the output of this vajram
 */
public abstract non-sealed class IOVajram<T> implements Vajram<T> {}
