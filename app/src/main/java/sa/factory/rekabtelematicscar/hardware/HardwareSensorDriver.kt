package sa.factory.rekabtelematicscar.hardware

import sa.factory.rekabtelematicscar.engine.AutonomousDigitalKeyLeaseEngine
import sa.factory.rekabtelematicscar.engine.ObdCanBusProtocolEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver(
    val obdEngine: ObdCanBusProtocolEngine = ObdCanBusProtocolEngine(),
    val leaseEngine: AutonomousDigitalKeyLeaseEngine = AutonomousDigitalKeyLeaseEngine()
) {
    private val peripheral = "OBD-II CAN-Bus Telematics + BLE Digital Key Fob"

    init {
        // Pre-warm sample simulation data
        val lease = leaseEngine.createRentalLease("CAR-01", "فيصل بن سلطان ال سعود")
        leaseEngine.initiateKeylessUnlock("CAR-01", proximityDbm = -58)
    }

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "Handshake initializing..."))
        delay(300)
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "OBD-II CAN-Bus Telematics + BLE Digital Key Fob: ONLINE | READY"))

        while (true) {
            delay(1500)
            val jitter = Random.nextInt(1, 6)
            val telem = obdEngine.generateLiveTelemetry(
                rawRpmA = 34 + jitter, rawRpmB = 120,
                rawSpeedA = 88 + jitter,
                rawCoolantA = 132,
                rawFuelA = 208,
                rawAmbientA = 74
            )
            val keyState = leaseEngine.getDigitalKeyState()
            val occupancy = leaseEngine.getFleetOccupancyRate()

            val payload = "OBD: ${telem.speedKmh} km/h (${telem.engineRpm} RPM) | Fuel: ${String.format("%.0f", telem.fuelLevelPct)}% | Key: $keyState | Fleet Rented: ${String.format("%.0f", occupancy)}% (Jitter: +${jitter}ms)"
            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", payload))
        }
    }
}
