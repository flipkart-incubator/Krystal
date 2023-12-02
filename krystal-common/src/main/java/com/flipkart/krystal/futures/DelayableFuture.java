package com.flipkart.krystal.futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A delayed future is one whose {@link #result} may NOT be available in the lifetime of the current
 * application process. It can be useful in modelling the result of a "delayable process" - a
 * process which can last much longer than any single application process (when humans are involved
 * in performing a process, for example).
 *
 * <p>For example:
 *
 * <pre>
 *   String emailId = "friend@example.com"
 *   String subject = "Trip plans?"
 *   String body = "Hey! When are you leaving from your home?"
 *   DelayedFuture&#60;EmailSendAck, EmailResponse&#62; emailResponse
 *        = sendEmail(emailId, subject, body);
 * </pre>
 *
 * In the above code, the "emailResponse" delayed future is created when we send an email. The
 * {@code EmailSendAck} is the acknowledgement from the email server that the mail has been received
 * by the server. Eventually the email will be sent to the reccipient who might respond to the
 * email, and this response might take a day or two. This means that the {@code EmailResponse} might
 * be ready long after the termination of this application process.
 *
 * <p>Frameworks can handle such scenarios by using a data store to persist the application state at
 * the point at which the email is sent. If and when eventually the response is received, the
 * framework can resume the execution of the application from that point on, this time with the
 * {@code EmailResponse} populated in the delayed Future. This resumption can happen in a completely
 * different process on a completely different piece of hardware. Since the response might never
 * come as well, frameworks should implement timeouts to avoid the execution from being frozen
 * completely.
 *
 * @param <A> the type representing acknowledgement that the delayable process has begun
 * @param <T> the type representing the result of the delayable process
 */
public abstract class DelayableFuture<A, T> extends CompletableFuture<T>
    implements CompletionStage<T> {}
