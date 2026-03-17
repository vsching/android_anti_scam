/**
 * Implementation of LinkCheckRepository using the 3-layer check architecture:
 * 1. Cache (Room) → 2. Local DB + Bloom filter → 3. Remote API fallback
 */
package com.safeanot.app.data.repository

import android.content.Context
import com.safeanot.app.data.local.ScamDomainDao
import com.safeanot.app.data.local.entity.CheckResultCacheEntity
import com.safeanot.app.data.remote.SafeAnotApi
import com.safeanot.app.data.remote.model.CheckRequest
import com.safeanot.app.domain.model.LinkVerdict
import com.safeanot.app.domain.model.VerdictType
import com.safeanot.app.domain.repository.LinkCheckRepository
import com.safeanot.app.util.BloomFilter
import com.safeanot.app.util.UrlNormalizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkCheckRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scamDomainDao: ScamDomainDao,
    private val api: SafeAnotApi,
) : LinkCheckRepository {

    companion object {
        private const val BLOOM_FILE_NAME = "bloom_filter.bin"
        private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000 // 24 hours
    }

    override suspend fun checkLink(input: String): LinkVerdict {
        // Layer 0: Normalize domain
        val domain = UrlNormalizer.normalize(input)
            ?: return LinkVerdict(
                domain = input,
                verdict = VerdictType.UNKNOWN,
                reason = "Invalid URL or domain",
                confidence = 0f,
            )

        val now = System.currentTimeMillis()

        // Layer 1: Check cache
        val cached = scamDomainDao.getValidCachedResult(domain, now)
        if (cached != null) {
            return LinkVerdict(
                domain = domain,
                verdict = parseVerdictType(cached.verdict),
                reason = cached.reason,
                confidence = cached.confidence,
            )
        }

        // Layer 2: Check Room scam domain table
        val localResult = scamDomainDao.findByDomain(domain)
        if (localResult != null) {
            return LinkVerdict(
                domain = domain,
                verdict = parseVerdictType(localResult.verdict),
                reason = localResult.reason,
                confidence = localResult.confidence,
            )
        }

        // Layer 3: Check Bloom filter
        val bloomFile = File(context.filesDir, BLOOM_FILE_NAME)
        if (bloomFile.exists()) {
            val bloomFilter = BloomFilter.loadFromBytes(bloomFile.readBytes())
            if (!bloomFilter.mightContain(domain)) {
                return LinkVerdict(
                    domain = domain,
                    verdict = VerdictType.NOT_IN_DATABASE,
                    reason = "Domain not found in database",
                    confidence = 1f,
                )
            }
        }

        // Layer 4: Bloom positive or no bloom → call API
        return try {
            val response = api.checkDomain(CheckRequest(domain = domain))

            // Cache the result
            scamDomainDao.upsertCachedResult(
                CheckResultCacheEntity(
                    domain = domain,
                    verdict = response.verdict,
                    reason = response.reason,
                    confidence = response.confidence,
                    cachedAt = now,
                    expiresAt = now + CACHE_TTL_MS,
                )
            )

            LinkVerdict(
                domain = domain,
                verdict = parseVerdictType(response.verdict),
                reason = response.reason,
                confidence = response.confidence,
            )
        } catch (_: IOException) {
            LinkVerdict(
                domain = domain,
                verdict = VerdictType.UNKNOWN,
                reason = "Unable to verify — offline",
                confidence = 0f,
            )
        } catch (_: Exception) {
            LinkVerdict(
                domain = domain,
                verdict = VerdictType.UNKNOWN,
                reason = "Unable to verify",
                confidence = 0f,
            )
        }
    }

    private fun parseVerdictType(verdict: String): VerdictType {
        return try {
            VerdictType.valueOf(verdict.uppercase())
        } catch (_: IllegalArgumentException) {
            VerdictType.UNKNOWN
        }
    }
}
