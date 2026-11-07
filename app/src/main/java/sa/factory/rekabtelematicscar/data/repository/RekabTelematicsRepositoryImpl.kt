package sa.factory.rekabtelematicscar.data.repository

import sa.factory.rekabtelematicscar.domain.repository.RekabTelematicsRepository
import sa.factory.rekabtelematicscar.hardware.HardwareSensorDriver
import sa.factory.rekabtelematicscar.model.RekabTelematicsItem
import sa.factory.rekabtelematicscar.model.RekabTelematicsOrder
import sa.factory.rekabtelematicscar.model.RekabTelematicsTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RekabTelematicsRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : RekabTelematicsRepository {

    private val _catalog = MutableStateFlow<List<RekabTelematicsItem>>(listOf(
        RekabTelematicsItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع OBD-II CAN-Bus Telematics + BLE Digital Key Fob", 180.0, "معتمد", "الخدمات الرئيسية"),
        RekabTelematicsItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        RekabTelematicsItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<RekabTelematicsItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<RekabTelematicsItem>>(emptyList())
    override val activeCart: StateFlow<List<RekabTelematicsItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<RekabTelematicsTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            RekabTelematicsTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: RekabTelematicsItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: RekabTelematicsItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): RekabTelematicsOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = RekabTelematicsOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
