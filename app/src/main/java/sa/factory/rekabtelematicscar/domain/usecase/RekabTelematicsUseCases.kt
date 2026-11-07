package sa.factory.rekabtelematicscar.domain.usecase

import sa.factory.rekabtelematicscar.domain.repository.RekabTelematicsRepository
import sa.factory.rekabtelematicscar.model.RekabTelematicsItem
import sa.factory.rekabtelematicscar.model.RekabTelematicsOrder
import sa.factory.rekabtelematicscar.model.RekabTelematicsTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: RekabTelematicsRepository) {
    operator fun invoke(): Flow<List<RekabTelematicsItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: RekabTelematicsRepository) {
    val cart = repository.activeCart
    fun add(item: RekabTelematicsItem) = repository.addToCart(item)
    fun remove(item: RekabTelematicsItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: RekabTelematicsRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): RekabTelematicsOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: RekabTelematicsRepository) {
    operator fun invoke(): Flow<RekabTelematicsTelemetry> = repository.telemetryFlow
}
