package se.ltu.emapal.compute.util.media.schema

import org.junit.Test

import java.nio.charset.StandardCharsets

import org.junit.Assert.*
import se.ltu.emapal.compute.util.media.json.JsonMediaConverter

class TestMediaReport {
    @Test
    fun shouldProvideViolationsGivenAtConstruction() {
        val violations = listOf(
                MediaViolation("x", MediaRequirement("req_a")),
                MediaViolation("y", MediaRequirement("req_b")),
                MediaViolation("z", MediaRequirement("req_c")))

        val report = MediaReport(violations)
        assertEquals(violations, report.violations)
    }

    @Test
    fun shouldReportValidityCorrectly() {
        assertFalse(MediaReport(listOf(
                MediaViolation("x", MediaRequirement("req_a")),
                MediaViolation("y", MediaRequirement("req_b")))).isValid)

        assertTrue(MediaReport(emptyList()).isValid)
    }

    @Test
    @Throws(Throwable::class)
    fun shouldEncodeCorrectly() {
        val report = MediaReport(listOf(
                MediaViolation("age", MediaRequirement("minimum", 0)),
                MediaViolation("name", MediaRequirement("regex", "[a-zA-Z ]{2,}"))))

        val json = JsonMediaConverter.encode(report.encodable).unwrap().toString(StandardCharsets.UTF_8)
        assertEquals("{\"is_valid\":false,\"violations\":[{\"entity\":\"age\",\"requirement\":{\"minimum\":[\"0\"]}},{\"entity\":\"name\",\"requirement\":{\"regex\":[\"[a-zA-Z ]{2,}\"]}}]}", json)
    }
}
