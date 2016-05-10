package se.ltu.emapal.compute;

import java.io.Closeable;
import java.io.IOException;

import rx.Observable;
import rx.subjects.PublishSubject;
import se.ltu.emapal.compute.util.Result;

/**
 * A context useful for executing Lua programs.
 */
@SuppressWarnings({"JniMissingFunction", "unused"})
public class ComputeContext implements Closeable {
    private static final Result.Success<Void, ComputeError> SUCCESS = new Result.Success<>(null);

    private final PublishSubject<ComputeLogEntry> logEntryPublishSubject = PublishSubject.create();
    private final Object lock = new Object();

    /**
     * Contains result of last call to {@link #processBatch(int, int, byte[])} or
     * {@link #registerLambda(int, String)} .
     */
    private ComputeError result = null;

    /**
     * Pointer to state managed by native methods.
     */
    private long nativePtr = 0;

    /**
     * Initializes new {@link ComputeContext} instance.
     */
    public ComputeContext() {
        construct();
    }

    /**
     * Processes provided batch, producing a new containing the result.
     */
    public Result<ComputeBatch, ComputeError> process(final ComputeBatch batch) {
        final int lambdaId = batch.getLambdaId();
        final int batchId = batch.getBatchId();
        final byte[] inData = batch.getData();
        synchronized (lock) {
            final byte[] outData = processBatch(lambdaId, batchId, inData);
            return result == null
                    ? new Result.Success<ComputeBatch, ComputeError>(new ComputeBatch(lambdaId, batchId, outData))
                    : new Result.Failure<ComputeBatch, ComputeError>(result);
        }
    }

    /**
     * Registers given lambda in compute context, allowing it to later be used to process batches.
     */
    public Result<Void, ComputeError> register(final ComputeLambda lambda) {
        final int lambdaId = lambda.getLambdaId();
        final String program = lambda.getProgram();
        synchronized (lock) {
            registerLambda(lambdaId, program);
            return result == null
                    ? SUCCESS
                    : new Result.Failure<Void, ComputeError>(result);
        }
    }

    /**
     * Observable pushing new log entries whenever the {@code lcm:log()} function is called by an
     * executed lambda.
     */
    public Observable<ComputeLogEntry> WhenLogEntry() {
        return logEntryPublishSubject;
    }

    @Override
    public void close() throws IOException {
        destroy();
    }

    /**
     * Called by JNI context after each call to {@link #processBatch(int, int, byte[])} and
     * {@link #registerLambda(int, String)}.
     * <p/>
     * Sets {@link #result} to {@code null} if the call was successful, or to some error if not.
     */
    private void onResult(final int code, final String message) {
        result = code != 0
                ? new ComputeError(code, message)
                : null;
    }

    /**
     * Called by Lua context when lambda invokes log function.
     */
    private void onLog(final int lambdaId, final int batchId, final String message) {
        logEntryPublishSubject.onNext(new ComputeLogEntry(lambdaId, batchId, message));
    }

    /**
     * Constructs any required native state.
     * <p/>
     * Must be called exactly once.
     */
    private native void construct();

    /**
     * Destroys any existing native state.
     * <p/>
     * Safe to call more than once.
     */
    private native void destroy();

    /**
     * Processes provided batch.
     * <p/>
     * Returns array of bytes if successful. Always calls {@link #onResult(int, String)} with the
     * result of the operation.
     */
    private native byte[] processBatch(int lambdaId, int batchId, byte[] data);

    /**
     * Registers given lambda in compute context, allowing it to later be used to process batches.
     * <p/>
     * If successful, sets error code to {@code 0}. Always calls {@link #onResult(int, String)}
     * with the result of the operation.
     */
    private native void registerLambda(int lambdaId, String program);

    static {
        System.loadLibrary("compute");
    }
}
