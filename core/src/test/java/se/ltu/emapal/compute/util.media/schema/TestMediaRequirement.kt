package se.ltu.emapal.compute.util.media.schema

import org.junit.Test

import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets

import org.junit.Assert.*
import se.ltu.emapal.compute.util.media.MediaDecoders
import se.ltu.emapal.compute.util.media.jackson.JacksonMediaConverter

class TestMediaRequirement {
    @Test
    fun shouldProvideNameAndParametersGivenAtConstruction() {
        val requirement = MediaRequirement("range", BigDecimal.valueOf(2.5), BigInteger.valueOf(1000))
        assertEquals("range", requirement.name)
        assertEquals(listOf("2.5", "1000"), requirement.parameters)
    }

    @Test
    fun shouldVerifyCorrectly() {
        val integer = MediaDecoders.ofNumber(120)

        val requirementSymbolic = MediaRequirement("maximum", 100)
        assertTrue(requirementSymbolic.predicate(integer))

        val requirementConcrete = MediaRequirement({ it.toLong() <= 100 }, "maximum", 100)
        assertFalse(requirementConcrete.predicate(integer))
    }

    @Test
    fun shouldEncodeCorrectly() {
        val requirement = MediaRequirement({ it.toLong() <= 100 }, "maximum", 100)
        val json = JacksonMediaConverter.encode(requirement.encodable).unwrap().toString(StandardCharsets.UTF_8)
        assertEquals("{\"maximum\":[\"100\"]}", json)
    }
}
