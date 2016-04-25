package se.ltu.emapal.compute

/**
 * A lambda, containing some Lua program able to process arbitrary byte array batches.
 *
 * @param lamdaId Lambda identifier unique within the application.
 * @param program Lua program.
 */
class ComputeLambda(
        val lamdaId: Int,
        val program: String
)
