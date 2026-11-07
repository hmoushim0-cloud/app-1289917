package sa.factory.rekabtelematicscar.domain.repository

import sa.factory.rekabtelematicscar.model.RekabTelematicsItem
import sa.factory.rekabtelematicscar.model.RekabTelematicsOrder
import sa.factory.rekabtelematicscar.model.RekabTelematicsTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RekabTelematicsRepository {
    val catalogFlow: Flow<List<RekabTelematicsItem>>
    val activeCart: StateFlow<List<RekabTelematicsItem>>
    val telemetryFlow: Flow<RekabTelematicsTelemetry>
    
    fun addToCart(item: RekabTelematicsItem)
    fun removeFromCart(item: RekabTelematicsItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): RekabTelematicsOrder
    suspend fun refreshCatalog()
}
