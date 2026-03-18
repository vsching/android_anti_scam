package com.safeanot.app.testutil

import com.safeanot.app.data.local.ReminderConfigDao
import com.safeanot.app.data.local.entity.ReminderConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeReminderConfigDao : ReminderConfigDao {
    private val configFlow = MutableStateFlow<ReminderConfigEntity?>(null)

    override fun getConfig(): Flow<ReminderConfigEntity?> = configFlow
    override suspend fun getConfigOnce(): ReminderConfigEntity? = configFlow.value
    override suspend fun upsert(config: ReminderConfigEntity) { configFlow.value = config }
    override suspend fun updateLastReminderDate(timestamp: Long) {}
}
