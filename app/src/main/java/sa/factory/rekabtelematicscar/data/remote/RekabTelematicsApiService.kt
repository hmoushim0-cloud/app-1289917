package sa.factory.rekabtelematicscar.data.remote

import sa.factory.rekabtelematicscar.model.RekabTelematicsItem
import sa.factory.rekabtelematicscar.model.RekabTelematicsOrder

interface RekabTelematicsApiService {
    suspend fun fetchCatalog(): List<RekabTelematicsItem>
    suspend fun submitOrder(order: RekabTelematicsOrder): Boolean
}
