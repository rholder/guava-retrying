package fr.free.jnizet.retry;

import java.util.concurrent.ExecutionException;

/**
 * An attempt of a call, which resulted either in a result returned by the call,
 * or in a Throwable thrown by the call.
 * @author JB
 *
 * @param <V> The type returned by the wrapped callable.
 */
public interface Attempt<V> {

    /**
     * Returns the result of the attempt, if any.
     * @return the result of the attempt
     * @throws ExecutionException if an exception was thrown by the attempt. The thrown
     * exception is set as the cause of the ExecutionException
     */
    public V get() throws ExecutionException;

    /**
     * Tells if the call returned a result or not
     * @return <code>true</code> if the call returned a result, <code>false</code>
     * if it threw an exception
     */
    public boolean hasResult();

    /**
     * Tells if the call threw an exception or not
     * @return <code>true</code> if the call threw an exception, <code>false</code>
     * if it returned a result
     */
    public boolean hasException();

    /**
     * Gets the result of the call
     * @return the result of the call
     * @throws IllegalStateException if the call didn't return a result, but threw an exception,
     * as indicated by {@link #hasResult()}
     */
    public V getResult() throws IllegalStateException;

    /**
     * Gets the exception thrown by the call
     * @return the exception thrown by the call
     * @throws IllegalStateException if the call didn't throw an exception,
     * as indicated by {@link #hasException()}
     */
    public Throwable getExceptionCause() throws IllegalStateException;
}
