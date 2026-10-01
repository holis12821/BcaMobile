package id.bca.bcamobile.data.config

import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.data.config.remote.HealthApi
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.common.map
import id.bca.bcamobile.domain.config.AppConfigRepository
import id.bca.bcamobile.domain.config.model.AppConfig
import id.bca.bcamobile.domain.config.model.FeatureFlags
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfigRepositoryImpl @Inject constructor(
    private val api: HealthApi,
    private val caller: ApiCaller,
) : AppConfigRepository {

    // Hanya di memory: config berlaku untuk satu sesi proses, dan menyimpannya ke
    // disk berarti aplikasi bisa memblokir nasabah berdasarkan status lama.
    @Volatile
    private var cached: AppConfig? = null

    override fun cached(): AppConfig? = cached

    override suspend fun refresh(): DataResult<AppConfig> =
        caller.call { api.health() }.map { dto ->
            AppConfig(
                isServiceHealthy = dto.service.equals(STATUS_OK, ignoreCase = true),
                maintenanceMode = dto.maintenanceMode,
                maintenanceMessage = dto.maintenanceMessage?.takeIf(String::isNotBlank),
                minimumAppVersion = dto.minimumAppVersion,
                forceUpdate = dto.forceUpdate,
                featureFlags = dto.featureFlags?.let {
                    FeatureFlags(
                        eWalletEnabled = it.eWalletEnabled,
                        qrisEnabled = it.qrisEnabled,
                        interbankTransferEnabled = it.interbankTransferEnabled,
                        virtualAccountEnabled = it.virtualAccountEnabled,
                    )
                } ?: FeatureFlags(),
            ).also { cached = it }
        }

    private companion object {
        const val STATUS_OK = "ok"
    }
}
