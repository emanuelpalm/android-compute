package se.ltu.emapal.compute.util.media.json

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.MediaEncodable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Provides JSON conversion utilities.
 */
object JsonMediaConverter {
    /** Jackson JSON factory used by encoder/decoder classes. */
    private val factory by lazy { JsonFactory() }

    /** Decodes provided input stream, returning . */
    fun decode(inputStream: InputStream): Result<MediaDecoder, IOException> {
        try {
            val objectMapper = ObjectMapper(factory)
            val node = objectMapper.readTree(inputStream)
            return Result.Success(JsonMediaDecoder(node))

        } catch (e: IOException) {
            return Result.Failure(e)
        }
    }

    /** Encodes provided value, writing any results to output stream. */
    fun encode(value: MediaEncodable, outputStream: OutputStream): Result<Void?, IOException> {
        try {
            JsonMediaEncoder(factory.createGenerator(outputStream)).use { encoder ->
                value.encode(encoder)
            }
            return Result.Success(null)

        } catch (e: IOException) {
            return Result.Failure(e)
        }
    }
}