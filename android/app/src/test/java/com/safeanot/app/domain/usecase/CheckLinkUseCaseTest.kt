package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.LinkVerdict
import com.safeanot.app.domain.model.VerdictType
import com.safeanot.app.domain.repository.LinkCheckRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CheckLinkUseCaseTest {

    private lateinit var useCase: CheckLinkUseCase
    private lateinit var fakeRepository: FakeLinkCheckRepository

    @Before
    fun setup() {
        fakeRepository = FakeLinkCheckRepository()
        useCase = CheckLinkUseCase(fakeRepository)
    }

    @Test
    fun `empty input returns UNKNOWN with empty input reason`() = runBlocking {
        val result = useCase("")
        assertEquals(VerdictType.UNKNOWN, result.verdict)
        assertEquals("Empty input", result.reason)
        assertEquals(0f, result.confidence)
    }

    @Test
    fun `blank input returns UNKNOWN with empty input reason`() = runBlocking {
        val result = useCase("   ")
        assertEquals(VerdictType.UNKNOWN, result.verdict)
        assertEquals("Empty input", result.reason)
    }

    @Test
    fun `valid input delegates to repository`() = runBlocking {
        val expectedVerdict = LinkVerdict(
            domain = "example.com",
            verdict = VerdictType.SAFE,
            reason = "Known safe domain",
            confidence = 0.95f,
        )
        fakeRepository.nextResult = expectedVerdict

        val result = useCase("https://example.com")
        assertEquals(VerdictType.SAFE, result.verdict)
        assertEquals("Known safe domain", result.reason)
        assertEquals(0.95f, result.confidence)
    }

    @Test
    fun `valid input passes original string to repository`() = runBlocking {
        val expectedVerdict = LinkVerdict(
            domain = "scam-site.com",
            verdict = VerdictType.DANGEROUS,
            reason = "Known scam",
            confidence = 0.99f,
        )
        fakeRepository.nextResult = expectedVerdict

        val input = "https://www.scam-site.com/fake-page"
        useCase(input)
        assertEquals(input, fakeRepository.lastInput)
    }

    /**
     * Simple fake implementation of LinkCheckRepository for testing.
     */
    private class FakeLinkCheckRepository : LinkCheckRepository {
        var nextResult: LinkVerdict = LinkVerdict(
            domain = "default.com",
            verdict = VerdictType.UNKNOWN,
            reason = "Default",
            confidence = 0f,
        )
        var lastInput: String? = null

        override suspend fun checkLink(input: String): LinkVerdict {
            lastInput = input
            return nextResult
        }
    }
}
