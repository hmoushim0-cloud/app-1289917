package sa.factory.rekabtelematicscar.di

import sa.factory.rekabtelematicscar.data.repository.RekabTelematicsRepositoryImpl
import sa.factory.rekabtelematicscar.domain.repository.RekabTelematicsRepository
import sa.factory.rekabtelematicscar.domain.usecase.*
import sa.factory.rekabtelematicscar.hardware.HardwareSensorDriver
import sa.factory.rekabtelematicscar.ui.viewmodel.RekabTelematicsViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: RekabTelematicsRepository = RekabTelematicsRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): RekabTelematicsViewModel = RekabTelematicsViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
