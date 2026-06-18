package uk.hristijan.pitstop.core.validation

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errors: List<String>) : ValidationResult
}

object VehicleValidator {
    fun validate(make: String, model: String, year: Int?, registration: String?): ValidationResult = resultOf(
        buildList {
            if (make.isBlank()) add("Make is required")
            if (model.isBlank()) add("Model is required")
            if (year != null && year !in 1886..2100) add("Year must be between 1886 and 2100")
            if (registration != null && registration.length > 32) add("Registration must be at most 32 characters")
        },
    )
}

object RefillValidator {
    fun validate(vehicleId: Long, timestamp: Long, odometerKm: Long, litres: Double, totalCostMinor: Long): ValidationResult = resultOf(
        buildList {
            if (vehicleId <= 0) add("Vehicle is required")
            if (timestamp <= 0) add("Timestamp must be positive")
            if (odometerKm < 0) add("Odometer cannot be negative")
            if (!litres.isFinite() || litres <= 0.0) add("Litres must be positive")
            if (totalCostMinor < 0) add("Cost cannot be negative")
        },
    )
}

object ServiceRecordValidator {
    fun validate(vehicleId: Long, timestamp: Long, title: String, odometerKm: Long?, totalCostMinor: Long): ValidationResult = resultOf(
        buildList {
            if (vehicleId <= 0) add("Vehicle is required")
            if (timestamp <= 0) add("Timestamp must be positive")
            if (title.isBlank()) add("Title is required")
            if (odometerKm != null && odometerKm < 0) add("Odometer cannot be negative")
            if (totalCostMinor < 0) add("Cost cannot be negative")
        },
    )
}

object FavoritePlaceValidator {
    fun validate(name: String, latitude: Double?, longitude: Double?): ValidationResult = resultOf(
        buildList {
            if (name.isBlank()) add("Name is required")
            if ((latitude == null) != (longitude == null)) add("Latitude and longitude must be provided together")
            if (latitude != null && (!latitude.isFinite() || latitude !in -90.0..90.0)) add("Latitude must be between -90 and 90")
            if (longitude != null && (!longitude.isFinite() || longitude !in -180.0..180.0)) add("Longitude must be between -180 and 180")
        },
    )
}

private fun resultOf(errors: List<String>): ValidationResult =
    if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
