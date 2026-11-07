package sa.factory.rekabtelematicscar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.rekabtelematicscar.domain.usecase.*
import sa.factory.rekabtelematicscar.model.RekabTelematicsItem
import sa.factory.rekabtelematicscar.model.RekabTelematicsOrder
import sa.factory.rekabtelematicscar.model.RekabTelematicsTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RekabTelematicsUiState(
    val catalog: List<RekabTelematicsItem> = emptyList(),
    val filteredCatalog: List<RekabTelematicsItem> = emptyList(),
    val selectedCategory: String = "الكل",
    val cart: List<RekabTelematicsItem> = emptyList(),
    val telemetry: RekabTelematicsTelemetry? = null,
    val lastOrder: RekabTelematicsOrder? = null,
    val isCheckingOut: Boolean = false,
    val activeTab: String = "home"
)

class RekabTelematicsViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val manageCartUseCase: ManageCartUseCase,
    private val processCheckoutUseCase: ProcessCheckoutUseCase,
    private val observeHardwareTelemetryUseCase: ObserveHardwareTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RekabTelematicsUiState())
    val uiState: StateFlow<RekabTelematicsUiState> = _uiState.asStateFlow()

    init {
        // Observe Catalog
        getCatalogUseCase().onEach { list ->
            _uiState.update { it.copy(catalog = list, filteredCatalog = list) }
        }.launchIn(viewModelScope)

        // Observe Cart
        manageCartUseCase.cart.onEach { cartItems ->
            _uiState.update { it.copy(cart = cartItems) }
        }.launchIn(viewModelScope)

        // Observe Telemetry
        observeHardwareTelemetryUseCase().onEach { telem ->
            _uiState.update { it.copy(telemetry = telem) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "الكل") state.catalog else state.catalog.filter { it.category == category }
            state.copy(selectedCategory = category, filteredCatalog = filtered)
        }
    }

    fun switchTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addToCart(item: RekabTelematicsItem) = manageCartUseCase.add(item)
    fun removeFromCart(item: RekabTelematicsItem) = manageCartUseCase.remove(item)

    fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }
            val order = processCheckoutUseCase()
            _uiState.update { it.copy(isCheckingOut = false, lastOrder = order, activeTab = "checkout") }
        }
    }
}
