package sa.factory.rekabtelematicscar.engine

import kotlin.math.ceil
import kotlin.math.max

enum class DigitalKeyState {
    REVOKED,
    ISSUED,
    PROXIMITY_UNLOCKED,
    ENGINE_IMMOBILIZER_DISENGAGED,
    LOCKED
}

data class FleetVehicle(
    val vehicleId: String,
    val modelName: String,
    val plateNumber: String,
    val hourlyRateSar: Double,
    val isAvailable: Boolean = true,
    val latitude: Double = 24.7136,
    val longitude: Double = 46.6753
) {
    val id: String get() = vehicleId
}

data class RentalLeaseContract(
    val contractId: String,
    val vehicleId: String,
    val customerName: String,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val totalHours: Int = 0,
    val totalAmountSar: Double = 0.0,
    val isSettled: Boolean = false
)

class AutonomousDigitalKeyLeaseEngine {
    // Riyadh Metropolitan Operational Boundary
    val minLat: Double = 24.40
    val maxLat: Double = 25.10
    val minLon: Double = 46.40
    val maxLon: Double = 47.10
    val maxSpeedLimitKmh: Int = 140

    private val fleet = mutableListOf(
        FleetVehicle("CAR-01", "لوسيد أير دريم إديشن (Lucid Air)", "أ ب د 1001", 120.0),
        FleetVehicle("CAR-02", "جينيسيس جي 90 رويال (Genesis G90)", "ر ك ب 2026", 95.0),
        FleetVehicle("CAR-03", "تويوتا لاندكروزر 300 VXR (Land Cruiser)", "ص م و 7777", 85.0),
        FleetVehicle("CAR-04", "مرسيدس بنز إس كلاس (Mercedes S-Class)", "ق م ر 5050", 150.0)
    )

    private val activeLeases = mutableListOf<RentalLeaseContract>()
    private var currentKeyState: DigitalKeyState = DigitalKeyState.LOCKED

    fun getFleet(): List<FleetVehicle> = fleet.toList()
    fun getActiveLeases(): List<RentalLeaseContract> = activeLeases.toList()
    fun getDigitalKeyState(): DigitalKeyState = currentKeyState

    fun findVehicleById(id: String): FleetVehicle? = fleet.find { it.vehicleId == id || it.id == id }

    fun isWithinGeofence(lat: Double, lon: Double): Boolean {
        return lat in minLat..maxLat && lon in minLon..maxLon
    }

    fun isSpeedExceeded(speedKmh: Int): Boolean = speedKmh > maxSpeedLimitKmh

    fun setDigitalKeyState(state: DigitalKeyState) {
        currentKeyState = state
    }

    fun initiateKeylessUnlock(vehicleId: String, proximityDbm: Int): Boolean {
        val vehicle = findVehicleById(vehicleId) ?: return false
        // Proximity threshold for keyless BLE entry (-70 dBm or stronger)
        if (proximityDbm >= -70) {
            currentKeyState = DigitalKeyState.PROXIMITY_UNLOCKED
            return true
        }
        return false
    }

    fun createRentalLease(vehicleId: String, customerName: String): RentalLeaseContract? {
        val vehicle = findVehicleById(vehicleId) ?: return null
        if (!vehicle.isAvailable) return null

        val contract = RentalLeaseContract(
            contractId = "LSE-${System.currentTimeMillis() % 100000}",
            vehicleId = vehicle.vehicleId,
            customerName = customerName,
            startTimestamp = System.currentTimeMillis()
        )
        activeLeases.add(contract)
        currentKeyState = DigitalKeyState.ISSUED

        val index = fleet.indexOfFirst { it.vehicleId == vehicleId }
        if (index != -1) {
            fleet[index] = fleet[index].copy(isAvailable = false)
        }
        return contract
    }

    fun settleRentalLease(contractId: String, durationMinutes: Long, fuelLevelPct: Double): RentalLeaseContract? {
        val index = activeLeases.indexOfFirst { it.contractId == contractId }
        if (index == -1) return null

        val lease = activeLeases[index]
        val vehicle = findVehicleById(lease.vehicleId) ?: return null

        val hours = max(1, ceil(durationMinutes / 60.0).toInt())
        var totalCost = hours * vehicle.hourlyRateSar

        // SAMA / Transport Authority standard: Fuel recovery penalty if returned < 80%
        if (fuelLevelPct < 80.0) {
            val missingPct = 80.0 - fuelLevelPct
            val fuelPenalty = missingPct * 1.5 // 1.5 SAR per missing percent
            totalCost += fuelPenalty
        }

        val settled = lease.copy(
            endTimestamp = System.currentTimeMillis(),
            totalHours = hours,
            totalAmountSar = totalCost,
            isSettled = true
        )
        activeLeases[index] = settled
        currentKeyState = DigitalKeyState.LOCKED

        val carIndex = fleet.indexOfFirst { it.vehicleId == lease.vehicleId }
        if (carIndex != -1) {
            fleet[carIndex] = fleet[carIndex].copy(isAvailable = true)
        }
        return settled
    }

    fun getFleetOccupancyRate(): Double {
        if (fleet.isEmpty()) return 0.0
        val rented = fleet.count { !it.isAvailable }
        return (rented.toDouble() / fleet.size.toDouble()) * 100.0
    }
}
