package sa.factory.rekabtelematicscar.engine

enum class VehicleHealthStatus {
    HEALTHY,
    WARNING,
    CRITICAL
}

data class ObdSensorTelemetry(
    val engineRpm: Int,
    val speedKmh: Int,
    val coolantTempCelsius: Int,
    val fuelLevelPct: Double,
    val ambientTempCelsius: Int,
    val distanceMilKm: Int,
    val activeDtcs: List<String>,
    val busLoadPct: Double,
    val healthStatus: VehicleHealthStatus,
    val timestamp: Long = System.currentTimeMillis()
)

class ObdCanBusProtocolEngine {
    private val activeDtcs = mutableListOf<String>()
    private var busLoadPct: Double = 14.2

    // SAE J1979 PID Formulas
    fun decodeEngineRpm(byteA: Int, byteB: Int): Int {
        // PID 0x0C: ((A * 256) + B) / 4
        return ((byteA * 256) + byteB) / 4
    }

    fun decodeVehicleSpeed(byteA: Int): Int {
        // PID 0x0D: A km/h
        return byteA
    }

    fun decodeCoolantTemp(byteA: Int): Int {
        // PID 0x05: A - 40 Celsius
        return byteA - 40
    }

    fun decodeFuelLevel(byteA: Int): Double {
        // PID 0x2F: (100 / 255) * A
        val level = (100.0 / 255.0) * byteA
        return if (level > 100.0) 100.0 else if (level < 0.0) 0.0 else level
    }

    fun decodeAmbientTemp(byteA: Int): Int {
        // PID 0x46: A - 40 Celsius
        return byteA - 40
    }

    fun decodeDistanceMil(byteA: Int, byteB: Int): Int {
        // PID 0x21: (A * 256) + B
        return (byteA * 256) + byteB
    }

    fun computeCrc16(data: ByteArray): Int {
        var crc = 0xFFFF
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            for (i in 0 until 8) {
                crc = if ((crc and 0x0001) != 0) {
                    (crc ushr 1) xor 0xA001
                } else {
                    crc ushr 1
                }
            }
        }
        return crc and 0xFFFF
    }

    fun addDiagnosticTroubleCode(dtc: String) {
        if (!activeDtcs.contains(dtc)) {
            activeDtcs.add(dtc)
        }
    }

    fun clearDiagnosticTroubleCodes() {
        activeDtcs.clear()
    }

    fun getActiveDtcs(): List<String> = activeDtcs.toList()

    fun evaluateHealthStatus(coolantTemp: Int, speed: Int): VehicleHealthStatus {
        return when {
            activeDtcs.isNotEmpty() || coolantTemp >= 115 -> VehicleHealthStatus.CRITICAL
            coolantTemp >= 105 || speed > 140 -> VehicleHealthStatus.WARNING
            else -> VehicleHealthStatus.HEALTHY
        }
    }

    fun generateLiveTelemetry(
        rawRpmA: Int = 32, rawRpmB: Int = 160,
        rawSpeedA: Int = 85,
        rawCoolantA: Int = 132, // 132 - 40 = 92C
        rawFuelA: Int = 204,    // ~80%
        rawAmbientA: Int = 72   // 32C
    ): ObdSensorTelemetry {
        val rpm = decodeEngineRpm(rawRpmA, rawRpmB)
        val speed = decodeVehicleSpeed(rawSpeedA)
        val coolant = decodeCoolantTemp(rawCoolantA)
        val fuel = decodeFuelLevel(rawFuelA)
        val ambient = decodeAmbientTemp(rawAmbientA)
        val health = evaluateHealthStatus(coolant, speed)

        return ObdSensorTelemetry(
            engineRpm = rpm,
            speedKmh = speed,
            coolantTempCelsius = coolant,
            fuelLevelPct = fuel,
            ambientTempCelsius = ambient,
            distanceMilKm = 0,
            activeDtcs = getActiveDtcs(),
            busLoadPct = busLoadPct,
            healthStatus = health
        )
    }
}
