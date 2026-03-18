/**
 * Repository implementation for scam alerts with API fetch + Room cache fallback.
 */
package com.safeanot.app.data.repository

import com.safeanot.app.data.local.AlertsDao
import com.safeanot.app.data.local.entity.AlertEntity
import com.safeanot.app.data.remote.SafeAnotApi
import com.safeanot.app.data.remote.model.AlertDto
import com.safeanot.app.domain.model.AlertRegion
import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.AlertSeverity
import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.repository.AlertsRepository
import com.safeanot.app.util.RegionResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class AlertsRepositoryImpl @Inject constructor(
    private val api: SafeAnotApi,
    private val alertsDao: AlertsDao,
) : AlertsRepository {

    override fun observeAlerts(filter: AlertRegionFilter): Flow<List<ScamAlert>> {
        val daoRegion = filter.toDaoRegion()
        val flow = if (daoRegion == null) {
            alertsDao.observeAlertsAll()
        } else {
            alertsDao.observeAlertsByRegion(daoRegion)
        }
        return flow.map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun refreshAlerts(filter: AlertRegionFilter) {
        val apiRegion = filter.toApiRegion()
        val dtos = api.getAlerts(region = apiRegion)
        val entities = dtos.map { it.toEntity() }

        if (apiRegion == null) {
            alertsDao.replaceAll(entities)
        } else {
            alertsDao.replaceByRegion(apiRegion, entities)
        }
    }

    override suspend fun getAlertById(id: String): ScamAlert? {
        return alertsDao.getAlertById(id)?.toDomain()
    }

    override fun getDefaultRegionFilter(): AlertRegionFilter {
        return RegionResolver.fromLocale()
    }

    private fun AlertDto.toEntity(): AlertEntity = AlertEntity(
        id = id,
        title = title,
        description = description,
        scamType = scamType,
        severity = severity,
        region = region,
        reportCount = reportCount,
        createdAt = createdAt,
    )

    private fun AlertEntity.toDomain(): ScamAlert = ScamAlert(
        id = id,
        title = title,
        description = description,
        scamType = scamType,
        severity = AlertSeverity.fromString(severity),
        region = AlertRegion.fromString(region),
        reportCount = reportCount,
        createdAt = createdAt,
        createdAtEpochMs = parseEpochMs(createdAt),
    )

    private fun parseEpochMs(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            format.parse(dateString)?.time ?: 0L
        } catch (_: Exception) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                format.parse(dateString)?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }
}
