package uk.hristijan.pitstop.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.hristijan.pitstop.core.validation.FavoritePlaceValidator
import uk.hristijan.pitstop.core.validation.RefillValidator
import uk.hristijan.pitstop.core.validation.ValidationResult
import uk.hristijan.pitstop.core.validation.VehicleValidator

class ValidatorsTest {
    @Test
    fun `valid vehicle passes validation`() {
        assertSame(ValidationResult.Valid, VehicleValidator.validate("Ford", "Focus", 2020, "SK-1234-AB"))
    }

    @Test
    fun `vehicle reports all invalid fields`() {
        val result = VehicleValidator.validate("", " ", 1700, "x".repeat(33))
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(4, (result as ValidationResult.Invalid).errors.size)
    }

    @Test
    fun `refill rejects invalid quantities`() {
        val result = RefillValidator.validate(0, 0, -1, Double.NaN, -1)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(5, (result as ValidationResult.Invalid).errors.size)
    }

    @Test
    fun `coordinates must be paired and within range`() {
        assertTrue(FavoritePlaceValidator.validate("Garage", 41.9, null) is ValidationResult.Invalid)
        assertTrue(FavoritePlaceValidator.validate("Garage", 91.0, 21.4) is ValidationResult.Invalid)
        assertSame(ValidationResult.Valid, FavoritePlaceValidator.validate("Garage", 41.9, 21.4))
    }
}
