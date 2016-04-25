package se.ltu.emapal.compute;

import se.ltu.emapal.compute.util.Result;

/**
 * A context useful for executing Lua programs.
 */
public class ComputeContext {
    private static final Result.Success<Void, ComputeError> SUCCESS = new Result.Success<>(null);
    private final Object lock = new Object();

    /**
     * Processes provided batch, producing a new containing the result.
     */
    public Result<ComputeBatch, ComputeError> process(final ComputeBatch batch) {
        final int lambdaId = batch.getLambdaId();
        final int batchId = batch.getBatchId();
        final byte[] inData = batch.getData();
        {
            final byte[] outData;
            final int errorCode;
            final String errorString;

            synchronized (lock) {
                outData = processBatch(lambdaId, batchId, inData);
                errorCode = errorCode();
                errorString = errorCode == 0 ? "" : errorString();
            }

            return errorCode == 0
                    ? new Result.Success<ComputeBatch, ComputeError>(new ComputeBatch(lambdaId, batchId, outData))
                    : new Result.Failure<ComputeBatch, ComputeError>(new ComputeError(errorCode, errorString));
        }
    }

    /**
     * Registers given lambda in compute context, allowing it to later be used to process batches.
     */
    public Result<Void, ComputeError> register(final ComputeLambda lambda) {
        final int lambdaId = lambda.getLambdaId();
        final String program = lambda.getProgram();
        {
            final int errorCode;
            final String errorString;

            synchronized (lock) {
                registerLambda(lambdaId, program);
                errorCode = errorCode();
                errorString = errorCode == 0 ? "" : errorString();
            }

            return errorCode() == 0
                    ? SUCCESS
                    : new Result.Failure<Void, ComputeError>(new ComputeError(errorCode, errorString));
        }
    }

    /**
     * Error code registered at last invocation of {@link #processBatch(int, int, byte[])} or
     * {@link #registerLambda(int, String)}.
     */
    private native int errorCode();

    /**
     * Error string registered at last invocation of {@link #processBatch(int, int, byte[])} or
     * {@link #registerLambda(int, String)}.
     */
    private native String errorString();

    /**
     * Processes provided batch.
     * <p/>
     * Returns array of bytes if successful. If not successful, sets appropriate error code and
     * string, and returns {@code null}.
     */
    private native byte[] processBatch(int lambdaId, int batchId, byte[] data);

    /**
     * Registers given lambda in compute context, allowing it to later be used to process batches.
     * <p/>
     * If successful, sets error code to {@code 0}. If not successful, sets appropriate error code
     * and string.
     */
    private native void registerLambda(int lambdaId, String program);
}
