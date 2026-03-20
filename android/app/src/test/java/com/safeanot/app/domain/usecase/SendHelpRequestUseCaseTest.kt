package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.AppCategory
import com.safeanot.app.domain.model.AuditItem
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.testutil.FakeAuditRepository
import com.safeanot.app.testutil.FakeGuardianRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SendHelpRequestUseCaseTest {

    private lateinit var fakeGuardianRepo: FakeGuardianRepository
    private lateinit var fakeAuditRepo: FakeAuditRepository
    private lateinit var useCase: SendHelpRequestUseCase

    @Before
    fun setup() {
        fakeGuardianRepo = FakeGuardianRepository()
        fakeAuditRepo = FakeAuditRepository()
        useCase = SendHelpRequestUseCase(fakeGuardianRepo, fakeAuditRepo)
    }

    @Test
    fun `sends correct payload with score and unfixed items`() = runTest {
        fakeAuditRepo.auditItemsFlow.value = listOf(
            AuditItem(
                id = 1,
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                category = AppCategory.MESSAGING,
                status = AuditStatus.SECURED,
            ),
            AuditItem(
                id = 2,
                packageName = "com.telegram",
                appName = "Telegram",
                category = AppCategory.MESSAGING,
                status = AuditStatus.NEEDS_REVIEW,
            ),
            AuditItem(
                id = 3,
                packageName = "com.chrome",
                appName = "Chrome",
                category = AppCategory.BROWSERS,
                status = AuditStatus.NOT_INSTALLED,
            ),
            AuditItem(
                id = 4,
                packageName = "com.firefox",
                appName = "Firefox",
                category = AppCategory.BROWSERS,
                status = AuditStatus.NEEDS_REVIEW,
            ),
        )

        useCase()

        assertTrue(fakeGuardianRepo.helpRequestSent)
        // 2 secured (SECURED + NOT_INSTALLED) out of 4 total = 50%
        assertEquals(50, fakeGuardianRepo.lastHelpRequestScore)
        // 2 unfixed items (NEEDS_REVIEW)
        val unfixed = fakeGuardianRepo.lastHelpRequestUnfixedItems
        assertNotNull(unfixed)
        assertEquals(2, unfixed!!.size)
        assertTrue(unfixed.contains("Telegram"))
        assertTrue(unfixed.contains("Firefox"))
    }

    @Test
    fun `sends empty unfixed items when all secured`() = runTest {
        fakeAuditRepo.auditItemsFlow.value = listOf(
            AuditItem(
                id = 1,
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                category = AppCategory.MESSAGING,
                status = AuditStatus.SECURED,
            ),
            AuditItem(
                id = 2,
                packageName = "com.chrome",
                appName = "Chrome",
                category = AppCategory.BROWSERS,
                status = AuditStatus.NOT_INSTALLED,
            ),
        )

        useCase()

        assertTrue(fakeGuardianRepo.helpRequestSent)
        assertEquals(100, fakeGuardianRepo.lastHelpRequestScore)
        assertEquals(emptyList<String>(), fakeGuardianRepo.lastHelpRequestUnfixedItems)
    }

    @Test(expected = RuntimeException::class)
    fun `propagates failure from repository`() = runTest {
        fakeAuditRepo.auditItemsFlow.value = listOf(
            AuditItem(
                id = 1,
                packageName = "com.test",
                appName = "Test",
                category = AppCategory.MESSAGING,
                status = AuditStatus.NEEDS_REVIEW,
            ),
        )
        fakeGuardianRepo.shouldThrow = true

        useCase()
    }
}
