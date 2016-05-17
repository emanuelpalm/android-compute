package se.ltu.emapal.compute.util.media.jackson

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.smile.SmileFactory
import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncoder
import java.io.*

/**
 * Provides JSON conversion utilities.
 *
 * @param factory Jackson format factory used by encoder/decoder classes.
 */
class JacksonMediaConverter private constructor(private val factory: JsonFactory) {
    /** Decodes provided byte array, returning [MediaDecoder] if successful. */
    fun decode(bytes: ByteArray): Result<MediaDecoder, IOException> {
        ByteArrayInputStream(bytes).use { input ->
            return decode(input)
        }
    }

    /** Decodes provided input stream, returning [MediaDecoder] if successful. */
    fun decode(input: InputStream): Result<MediaDecoder, IOException> {
        try {
            val objectMapper = ObjectMapper(factory)
            val node = objectMapper.readTree(input)
            return Result.Success(JacksonMediaDecoder(node))

        } catch (e: IOException) {
            return Result.Failure(e)
        }
    }

    /** Encodes provided encodable into returned byte array, if successful. */
    fun encode(encodable: MediaEncodable): Result<ByteArray, IOException> {
        return encode(encodable.encodable)
    }

    /** Encodes provided encodable into returned byte array, if successful. */
    fun encode(encodable: (MediaEncoder) -> Unit): Result<ByteArray, IOException> {
        ByteArrayOutputStream().use { output ->
            return encode(encodable, output)
                    .map { output.toByteArray() }
        }
    }

    /** Encodes provided encodable, writing any results to output stream. */
    fun encode(encodable: MediaEncodable, output: OutputStream): Result<Void?, IOException> {
        return encode(encodable.encodable, output)
    }

    /** Encodes provided encodable, writing any results to output stream. */
    fun encode(encodable: (MediaEncoder) -> Unit, output: OutputStream): Result<Void?, IOException> {
        try {
            JacksonMediaEncoder(factory.createGenerator(output)).use { encoder ->
                encodable(encoder)
            }
            return Result.Success(null)

        } catch (e: IOException) {
            return Result.Failure(e)
        }
    }

    companion object {
        /** [JacksonMediaConverter] for managing JSON data. */
        val JSON by lazy {
            JacksonMediaConverter(JsonFactory())
        }

        /** [JacksonMediaConverter] for managing SMILE data. */
        val SMILE by lazy {
            JacksonMediaConverter(SmileFactory())
        }
    }
}