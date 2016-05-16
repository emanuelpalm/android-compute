package se.ltu.emapal.compute.util.media.schema

import org.junit.Test

import org.junit.Assert.assertEquals
import se.ltu.emapal.compute.util.media.json.JsonMediaConverter
import java.nio.charset.StandardCharsets

class TestMediaViolation {
    @Test
    fun shouldProvideEntityIdentifierAndRequirementGivenAtConstruction() {
        val entityName = "x"
        val requirement = MediaRequirement("required")

        val violation = MediaViolation(entityName, requirement)
        assertEquals(entityName, violation.entity)
        assertEquals(requirement, violation.requirement)
    }

    @Test
    fun shouldEncodeCorrectly() {
        val violation = MediaViolation("x", MediaRequirement("range", 10, 100))
        val json = JsonMediaConverter.encode(violation).unwrap().toString(StandardCharsets.UTF_8)
        assertEquals("{\"entity\":\"x\",\"requirement\":{\"range\":[\"10\",\"100\"]}}", json)
    }
}
