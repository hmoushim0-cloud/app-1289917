package sa.factory.rekabtelematicscar

import sa.factory.rekabtelematicscar.data.repository.RekabTelematicsRepositoryImpl
import sa.factory.rekabtelematicscar.engine.AutonomousDigitalKeyLeaseEngine
import sa.factory.rekabtelematicscar.engine.DigitalKeyState
import sa.factory.rekabtelematicscar.engine.ObdCanBusProtocolEngine
import sa.factory.rekabtelematicscar.engine.VehicleHealthStatus
import sa.factory.rekabtelematicscar.hardware.HardwareSensorDriver
import sa.factory.rekabtelematicscar.model.RekabTelematicsItem
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RekabTelematicsRegressionTest {

    @Test
    fun testCheckoutCalculationAndTax() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = RekabTelematicsRepositoryImpl(sensorDriver)

        val item1 = RekabTelematicsItem("t1", "خدمة تجريبية أ", "الوصف", 100.0, "معتمد", "عام")
        val item2 = RekabTelematicsItem("t2", "خدمة تجريبية ب", "الوصف", 200.0, "معتمد", "عام")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada EMV")

        assertEquals(300.0, order.subtotalSar, 0.01)
        assertEquals(45.0, order.vatSar, 0.01)
        assertEquals(345.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
    }

    @Test
    fun testObdEngineRpmDecodingPid() {
        val obd = ObdCanBusProtocolEngine()
        // ((32 * 256) + 160) / 4 = (8192 + 160) / 4 = 8352 / 4 = 2088 RPM
        val rpm = obd.decodeEngineRpm(32, 160)
        assertEquals(2088, rpm)
    }

    @Test
    fun testObdVehicleSpeedAndCoolantTempPids() {
        val obd = ObdCanBusProtocolEngine()
        val speed = obd.decodeVehicleSpeed(110)
        assertEquals(110, speed)

        // Coolant: A - 40 Celsius -> 132 - 40 = 92C
        val temp = obd.decodeCoolantTemp(132)
        assertEquals(92, temp)
    }

    @Test
    fun testObdFuelLevelCalculation() {
        val obd = ObdCanBusProtocolEngine()
        // (100 / 255) * 204 = 80.0%
        val fuel = obd.decodeFuelLevel(204)
        assertEquals(80.0, fuel, 0.1)

        // Clamping tests
        val fuelMax = obd.decodeFuelLevel(255)
        assertEquals(100.0, fuelMax, 0.01)
        val fuelMin = obd.decodeFuelLevel(0)
        assertEquals(0.0, fuelMin, 0.01)
    }

    @Test
    fun testObdCrc16IntegrityCalculation() {
        val obd = ObdCanBusProtocolEngine()
        val sampleFrame = byteArrayOf(0x02, 0x01, 0x0C, 0x55)
        val crc1 = obd.computeCrc16(sampleFrame)
        val crc2 = obd.computeCrc16(sampleFrame)

        assertEquals("Same payload must produce identical CRC", crc1, crc2)
        assertTrue(crc1 > 0)
    }

    @Test
    fun testObdDiagnosticTroubleCodeEvaluation() {
        val obd = ObdCanBusProtocolEngine()
        assertEquals(0, obd.getActiveDtcs().size)
        assertEquals(VehicleHealthStatus.HEALTHY, obd.evaluateHealthStatus(coolantTemp = 90, speed = 100))

        // Inject DTC code
        obd.addDiagnosticTroubleCode("P0300")
        assertEquals(1, obd.getActiveDtcs().size)
        assertEquals(VehicleHealthStatus.CRITICAL, obd.evaluateHealthStatus(coolantTemp = 90, speed = 100))

        obd.clearDiagnosticTroubleCodes()
        assertEquals(0, obd.getActiveDtcs().size)
    }

    @Test
    fun testFleetVehicleRosterPreload() {
        val leaseEngine = AutonomousDigitalKeyLeaseEngine()
        val fleet = leaseEngine.getFleet()
        assertEquals(4, fleet.size)

        val car = leaseEngine.findVehicleById("CAR-01")
        assertNotNull(car)
        assertTrue(car!!.modelName.contains("Lucid Air"))
        assertEquals(120.0, car.hourlyRateSar, 0.01)
    }

    @Test
    fun testRiyadhGeofenceBoundaryValidation() {
        val leaseEngine = AutonomousDigitalKeyLeaseEngine()
        // King Fahd Road / Olaya: (24.71, 46.67) -> Inside Riyadh
        assertTrue(leaseEngine.isWithinGeofence(24.7136, 46.6753))

        // Jeddah coordinates: (21.54, 39.17) -> Outside Riyadh Geofence
        assertFalse(leaseEngine.isWithinGeofence(21.5433, 39.1728))

        // Speed limit check
        assertFalse(leaseEngine.isSpeedExceeded(120))
        assertTrue(leaseEngine.isSpeedExceeded(145))
    }

    @Test
    fun testDigitalKeyUnlockAndLeaseSettlement() {
        val leaseEngine = AutonomousDigitalKeyLeaseEngine()

        // Test keyless BLE unlock with strong proximity (-60 dBm >= -70 dBm)
        val unlocked = leaseEngine.initiateKeylessUnlock("CAR-02", proximityDbm = -60)
        assertTrue(unlocked)
        assertEquals(DigitalKeyState.PROXIMITY_UNLOCKED, leaseEngine.getDigitalKeyState())

        // Create lease
        val lease = leaseEngine.createRentalLease("CAR-02", "سلطان التميمي")
        assertNotNull(lease)
        assertEquals(DigitalKeyState.ISSUED, leaseEngine.getDigitalKeyState())

        // Settle lease after 120 minutes (2 hours @ 95 SAR = 190 SAR) with full tank (85% >= 80%)
        val settled = leaseEngine.settleRentalLease(lease!!.contractId, durationMinutes = 120, fuelLevelPct = 85.0)
        assertNotNull(settled)
        assertTrue(settled!!.isSettled)
        assertEquals(2, settled.totalHours)
        assertEquals(190.0, settled.totalAmountSar, 0.01)
        assertEquals(DigitalKeyState.LOCKED, leaseEngine.getDigitalKeyState())
    }

    @Test
    fun testHardwareSensorDriverLiveStreamFrames() = runBlocking {
        val driver = HardwareSensorDriver()
        val frames = driver.observePeripheralStream().take(2).toList()

        assertEquals("CONNECTING", frames[0].connectionState)
        assertEquals("ONLINE", frames[1].connectionState)
        assertTrue(frames[1].livePayload.contains("OBD-II CAN-Bus"))
    }
}
