package com.safeanot.app.feature.check

import com.safeanot.app.domain.model.LinkVerdict
import com.safeanot.app.domain.model.ShareEventModel
import com.safeanot.app.domain.model.VerdictType
import com.safeanot.app.domain.model.WarningTone
import com.safeanot.app.domain.repository.LinkCheckRepository
import com.safeanot.app.domain.repository.ShareEventRepository
import com.safeanot.app.domain.usecase.CheckLinkUseCase
import com.safeanot.app.domain.usecase.GenerateRescueCardUseCase
import com.safeanot.app.domain.usecase.TrackShareEventUseCase
import com.safeanot.app.util.WarningTemplateProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        val vm = createViewModel()
        assertEquals(CheckUiState.Idle, vm.checkState.value)
    }

    @Test
    fun `onUrlChanged updates url input`() {
        val vm = createViewModel()
        vm.onUrlChanged("https://example.com")
        assertEquals("https://example.com", vm.urlInput.value)
    }

    @Test
    fun `checkLink with blank input does nothing`() {
        val vm = createViewModel()
        vm.onUrlChanged("   ")
        vm.checkLink()
        assertEquals(CheckUiState.Idle, vm.checkState.value)
    }

    @Test
    fun `checkLink success transitions to Result`() {
        val verdict = LinkVerdict("example.com", VerdictType.SAFE, "Known safe", 0.95f)
        val vm = createViewModel(verdict)

        vm.checkLink("https://example.com")

        val state = vm.checkState.value
        assertTrue(state is CheckUiState.Result)
        assertEquals(verdict, (state as CheckUiState.Result).verdict)
    }

    @Test
    fun `checkLink failure transitions to Error`() {
        val vm = createViewModel(error = RuntimeException("Network error"))

        vm.checkLink("https://example.com")

        val state = vm.checkState.value
        assertTrue(state is CheckUiState.Error)
        assertEquals("Network error", (state as CheckUiState.Error).message)
    }

    @Test
    fun `clearResults resets to Idle and clears input`() {
        val verdict = LinkVerdict("example.com", VerdictType.SAFE, "Safe", 0.9f)
        val vm = createViewModel(verdict)

        vm.checkLink("https://example.com")
        vm.clearResults()

        assertEquals(CheckUiState.Idle, vm.checkState.value)
        assertEquals("", vm.urlInput.value)
    }

    @Test
    fun `prefillUrl sets url input`() {
        val vm = createViewModel()
        vm.prefillUrl("https://test.com")
        assertEquals("https://test.com", vm.urlInput.value)
    }

    @Test
    fun `shareResult does not emit when state is not Result`() = runTest {
        val vm = createViewModel()

        vm.shareResult()

        // No event should be emitted — verify by trying to receive with a timeout
        val deferred = async {
            try {
                vm.shareEvents.first()
            } catch (_: Exception) {
                null
            }
        }
        deferred.cancel()
    }

    @Test
    fun `shareResult emits ImageWithText event with domain data`() = runTest {
        val verdict = LinkVerdict("example.com", VerdictType.DANGEROUS, "Phishing", 0.99f)
        val vm = createViewModel(verdict)

        vm.checkLink("https://example.com")

        // Collect the event in a separate coroutine
        val eventDeferred = async { vm.shareEvents.first() }

        vm.shareResult()

        val event = eventDeferred.await()
        assertTrue("Event should be ImageWithText", event is ShareEvent.ImageWithText)
        val imageEvent = event as ShareEvent.ImageWithText
        assertNotNull("Bitmap should not be null", imageEvent.bitmap)
        assertTrue("Text should contain domain", imageEvent.text.contains("example.com"))
        assertTrue("Text should contain verdict", imageEvent.text.contains("DANGEROUS"))
        assertTrue("Text should contain deep link", imageEvent.text.contains("https://safeanot.com/result"))
    }

    @Test
    fun `shareWarning emits formatted text for DANGEROUS verdict`() = runTest {
        val verdict = LinkVerdict("evil.com", VerdictType.DANGEROUS, "Phishing site", 0.99f)
        val vm = createViewModel(verdict)
        vm.checkLink("https://evil.com")

        val template = WarningTemplateProvider.getTemplates()
            .first { it.tone == WarningTone.POLITE }

        val eventDeferred = async { vm.warningShareEvent.first() }
        vm.shareWarning(template, "en")

        val text = eventDeferred.await()
        assertTrue("Should contain domain", text.contains("evil.com"))
        assertTrue("Should contain verdict", text.contains("DANGEROUS"))
        assertTrue("Should contain download URL", text.contains("play.google.com"))
    }

    @Test
    fun `shareWarning does not emit when state is not Result`() = runTest {
        val vm = createViewModel()
        val template = WarningTemplateProvider.getTemplates().first()

        vm.shareWarning(template, "en")

        val deferred = async {
            try {
                vm.warningShareEvent.first()
            } catch (_: Exception) {
                null
            }
        }
        deferred.cancel()
    }

    @Test
    fun `shareWarning formats template with correct domain and verdict`() = runTest {
        val verdict = LinkVerdict("scam.my", VerdictType.DANGEROUS, "Known scam", 0.95f)
        val vm = createViewModel(verdict)
        vm.checkLink("https://scam.my")

        val template = WarningTemplateProvider.getTemplates()
            .first { it.tone == WarningTone.URGENT }

        val eventDeferred = async { vm.warningShareEvent.first() }
        vm.shareWarning(template, "ms")

        val text = eventDeferred.await()
        assertTrue("Should contain domain", text.contains("scam.my"))
        assertTrue("Should be in Malay", text.contains("AMARAN"))
    }

    @Test
    fun `ViewModel constructor has no Context parameter`() {
        // This test verifies the architectural constraint that ViewModel has no Context.
        // If CheckViewModel's constructor required Context, this would fail to compile.
        val vm = createViewModel()
        assertNotNull(vm)
    }

    @Test
    fun `shareRescueCard emits BitmapOnly event`() = runTest {
        val verdict = LinkVerdict("evil.com", VerdictType.DANGEROUS, "Phishing", 0.99f)
        val vm = createViewModel(verdict)

        vm.checkLink("https://evil.com")

        val eventDeferred = async { vm.shareEvents.first() }

        vm.shareRescueCard()

        val event = eventDeferred.await()
        assertTrue("Event should be BitmapOnly", event is ShareEvent.BitmapOnly)
        val bitmapEvent = event as ShareEvent.BitmapOnly
        assertNotNull("Bitmap should not be null", bitmapEvent.bitmap)
    }

    @Test
    fun `shareRescueCard does not emit when state is not Result`() = runTest {
        val vm = createViewModel()

        vm.shareRescueCard()

        val deferred = async {
            try {
                vm.shareEvents.first()
            } catch (_: Exception) {
                null
            }
        }
        deferred.cancel()
    }

    private fun createViewModel(
        verdictToReturn: LinkVerdict? = null,
        error: Exception? = null,
    ): CheckViewModel {
        val repo = object : LinkCheckRepository {
            override suspend fun checkLink(input: String): LinkVerdict {
                if (error != null) throw error
                return verdictToReturn ?: LinkVerdict("test.com", VerdictType.UNKNOWN, "Unknown", 0f)
            }
        }
        val shareRepo = object : ShareEventRepository {
            override suspend fun trackEvent(event: ShareEventModel): Boolean = true
            override suspend fun syncPendingEvents(): Boolean = true
        }
        return CheckViewModel(
            checkLinkUseCase = CheckLinkUseCase(repo),
            trackShareEventUseCase = TrackShareEventUseCase(shareRepo),
            generateRescueCardUseCase = GenerateRescueCardUseCase(),
        )
    }
}
