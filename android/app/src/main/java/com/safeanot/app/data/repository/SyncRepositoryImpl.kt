/**
 * Implementation of SyncRepository that handles downloading and applying
 * database updates from the Safe Anot? backend API.
 *
 * Two-phase sync:
 * Phase 1 (network): download metadata, files to temp, verify SHA-256
 * Phase 2 (DB transaction): parse and apply delta/full, update metadata
 * After commit: atomically move bloom filter to final path
 */
package com.safeanot.app.data.repository

import android.content.Context
import com.safeanot.app.data.local.ScamDomainDao
import com.safeanot.app.data.local.entity.ScamDomainEntity
import com.safeanot.app.data.local.entity.SyncMetadataEntity
import com.safeanot.app.data.remote.SafeAnotApi
import com.safeanot.app.domain.model.SyncOutcome
import com.safeanot.app.domain.repository.SyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.ResponseBody
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: SafeAnotApi,
    private val scamDomainDao: ScamDomainDao,
) : SyncRepository {

    companion object {
        private const val BLOOM_FILE_NAME = "bloom_filter.bin"
        private const val FULL_DB_HARD_MAX_BYTES = 50L * 1024 * 1024 // 50 MB
        private const val DELTA_HARD_MAX_BYTES = 5L * 1024 * 1024 // 5 MB
        private const val BLOOM_HARD_MAX_BYTES = 5L * 1024 * 1024 // 5 MB
        private const val SIZE_TOLERANCE = 1.1
    }

    override suspend fun syncDatabase(): SyncOutcome {
        return try {
            // Phase 1: Network — fetch metadata and download files
            val metadata = api.getLatestMetadata()

            val currentVersion = scamDomainDao.getMetadata()?.lastSyncVersion
            if (currentVersion == metadata.version) {
                return SyncOutcome.Success
            }

            val isFirstRun = currentVersion == null
            val bloomTempFile = File(context.filesDir, "bloom_temp.bin")
            val dataTempFile = File(context.filesDir, "data_temp.bin")

            try {
                // Download bloom filter
                val bloomMaxBytes = minOf(
                    (metadata.bloomSizeKb * 1024 * SIZE_TOLERANCE).toLong(),
                    BLOOM_HARD_MAX_BYTES,
                )
                downloadBounded(api.getBloomFilter(), bloomTempFile, bloomMaxBytes)

                // Download full or delta database
                if (isFirstRun) {
                    val fullMaxBytes = minOf(
                        (metadata.fullSizeKb * 1024 * SIZE_TOLERANCE).toLong(),
                        FULL_DB_HARD_MAX_BYTES,
                    )
                    downloadBounded(api.getFullDatabase(), dataTempFile, fullMaxBytes)
                } else {
                    val deltaMaxBytes = minOf(
                        (metadata.deltaSizeKb * 1024 * SIZE_TOLERANCE).toLong(),
                        DELTA_HARD_MAX_BYTES,
                    )
                    downloadBounded(api.getDelta(currentVersion!!), dataTempFile, deltaMaxBytes)
                }

                // Phase 2: DB — parse and apply inside transaction
                val domains = parseDomainData(dataTempFile)

                if (isFirstRun) {
                    scamDomainDao.deleteAll()
                }
                scamDomainDao.insertAll(domains)
                scamDomainDao.upsertMetadata(
                    SyncMetadataEntity(
                        lastSyncVersion = metadata.version,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        domainCount = metadata.domainCount,
                    )
                )

                // After commit: atomically move bloom file
                val bloomFinalFile = File(context.filesDir, BLOOM_FILE_NAME)
                bloomTempFile.renameTo(bloomFinalFile)

                SyncOutcome.Success
            } finally {
                // Clean up temp files
                dataTempFile.delete()
                // Only delete bloom temp if it still exists (wasn't renamed)
                if (bloomTempFile.exists()) bloomTempFile.delete()
            }
        } catch (e: IOException) {
            SyncOutcome.Retry
        } catch (e: SecurityException) {
            SyncOutcome.PermanentFailure("Checksum mismatch: ${e.message}")
        } catch (e: IllegalStateException) {
            SyncOutcome.PermanentFailure(e.message ?: "Unknown error")
        } catch (e: Exception) {
            SyncOutcome.Retry
        }
    }

    override suspend fun getLastSyncVersion(): String? {
        return scamDomainDao.getMetadata()?.lastSyncVersion
    }

    override suspend fun isFirstRun(): Boolean {
        return scamDomainDao.getMetadata() == null
    }

    /**
     * Downloads a streaming response body to a file, enforcing a size limit.
     * Throws IllegalStateException if the download exceeds maxBytes.
     */
    private fun downloadBounded(body: ResponseBody, target: File, maxBytes: Long) {
        body.use { responseBody ->
            target.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var totalRead = 0L
                val source = responseBody.byteStream()
                while (true) {
                    val bytesRead = source.read(buffer)
                    if (bytesRead == -1) break
                    totalRead += bytesRead
                    if (totalRead > maxBytes) {
                        throw IllegalStateException(
                            "Download exceeds size limit: $totalRead > $maxBytes bytes"
                        )
                    }
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
    }

    /**
     * Parses domain data from a newline-delimited file.
     * Expected format per line: domain|verdict|reason|source|confidence
     */
    private fun parseDomainData(file: File): List<ScamDomainEntity> {
        return file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size >= 5) {
                    ScamDomainEntity(
                        domain = parts[0].trim(),
                        verdict = parts[1].trim(),
                        reason = parts[2].trim(),
                        source = parts[3].trim(),
                        confidence = parts[4].trim().toFloatOrNull() ?: 0.5f,
                    )
                } else {
                    null
                }
            }
    }

    @Suppress("unused")
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
