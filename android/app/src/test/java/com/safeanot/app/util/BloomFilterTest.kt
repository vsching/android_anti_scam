package com.safeanot.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BloomFilterTest {

    /**
     * Build a Bloom filter binary from a set of domains using the SAFB format.
     */
    private fun buildBloomFilterBytes(
        domains: Set<String>,
        bitCount: Int,
        hashCount: Int,
    ): ByteArray {
        val byteCount = (bitCount + 7) / 8
        val bitArray = ByteArray(byteCount)

        for (domain in domains) {
            val key = domain.toByteArray(Charsets.UTF_8)
            val hash1 = BloomFilter.murmurHash3(key, 0)
            val hash2 = BloomFilter.murmurHash3(key, hash1.toInt())
            for (i in 0 until hashCount) {
                val combined = hash1 + i.toLong() * hash2
                val bitIndex = ((combined % bitCount + bitCount) % bitCount).toInt()
                val byteIndex = bitIndex / 8
                val bitOffset = bitIndex % 8
                bitArray[byteIndex] = (bitArray[byteIndex].toInt() or (1 shl bitOffset)).toByte()
            }
        }

        val header = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        header.put(0x53.toByte()) // S
        header.put(0x41.toByte()) // A
        header.put(0x46.toByte()) // F
        header.put(0x42.toByte()) // B
        header.putInt(1) // version
        header.putInt(bitCount)
        header.putInt(hashCount)
        header.putInt(domains.size)
        // 12 bytes reserved (already zeros)
        header.put(ByteArray(12))

        return header.array() + bitArray
    }

    @Test
    fun `known domains are found in bloom filter`() {
        val domains = setOf(
            "scam-example.com",
            "phishing-site.net",
            "malware-download.org",
            "fake-bank.com",
            "lottery-scam.biz",
        )
        val data = buildBloomFilterBytes(domains, bitCount = 1024, hashCount = 7)
        val filter = BloomFilter.loadFromBytes(data)

        assertEquals(5, filter.domainCount)
        for (domain in domains) {
            assertTrue("Expected $domain to be found", filter.mightContain(domain))
        }
    }

    @Test
    fun `unknown domains are mostly not found`() {
        val domains = setOf(
            "scam-example.com",
            "phishing-site.net",
            "malware-download.org",
        )
        val data = buildBloomFilterBytes(domains, bitCount = 4096, hashCount = 7)
        val filter = BloomFilter.loadFromBytes(data)

        val testDomains = (1..100).map { "safe-domain-$it.com" }
        val falsePositives = testDomains.count { filter.mightContain(it) }

        // With 3 domains in a 4096-bit filter with 7 hashes, FPR should be well under 1%
        assertTrue(
            "False positive rate too high: $falsePositives/100",
            falsePositives < 1,
        )
    }

    @Test
    fun `FPR under 1 percent with realistic parameters`() {
        // Insert 1000 domains, use optimal parameters for <1% FPR
        // For n=1000, FPR<1%: m=9585 bits, k=7
        val domains = (1..1000).map { "scam-domain-$it.example.com" }.toSet()
        val data = buildBloomFilterBytes(domains, bitCount = 9585, hashCount = 7)
        val filter = BloomFilter.loadFromBytes(data)

        // All inserted domains must be found
        for (domain in domains) {
            assertTrue("Inserted domain not found: $domain", filter.mightContain(domain))
        }

        // Check FPR with 10000 non-inserted domains
        val testDomains = (1..10000).map { "legitimate-site-$it.com" }
        val falsePositives = testDomains.count { filter.mightContain(it) }
        val fpr = falsePositives.toDouble() / testDomains.size

        assertTrue(
            "FPR too high: ${"%.4f".format(fpr)} ($falsePositives/10000)",
            fpr < 0.01,
        )
    }

    @Test
    fun `header parsing extracts correct values`() {
        val domains = setOf("test.com")
        val data = buildBloomFilterBytes(domains, bitCount = 256, hashCount = 5)
        val filter = BloomFilter.loadFromBytes(data)

        assertEquals(1, filter.domainCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `corrupted header - too small`() {
        BloomFilter.loadFromBytes(ByteArray(10))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `corrupted header - bad magic`() {
        val data = ByteArray(64)
        data[0] = 0x00
        data[1] = 0x00
        data[2] = 0x00
        data[3] = 0x00
        BloomFilter.loadFromBytes(data)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `corrupted header - unsupported version`() {
        val header = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
        header.put(0x53.toByte())
        header.put(0x41.toByte())
        header.put(0x46.toByte())
        header.put(0x42.toByte())
        header.putInt(99) // bad version
        header.putInt(64)
        header.putInt(3)
        header.putInt(0)
        BloomFilter.loadFromBytes(header.array())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `corrupted header - data too small for bit array`() {
        val header = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        header.put(0x53.toByte())
        header.put(0x41.toByte())
        header.put(0x46.toByte())
        header.put(0x42.toByte())
        header.putInt(1) // version
        header.putInt(1024) // 1024 bits = 128 bytes needed
        header.putInt(3)
        header.putInt(0)
        header.put(ByteArray(12))
        // Only 32 bytes total, no room for the 128-byte bit array
        BloomFilter.loadFromBytes(header.array())
    }

    @Test
    fun `empty bloom filter contains nothing`() {
        val data = buildBloomFilterBytes(emptySet(), bitCount = 256, hashCount = 5)
        val filter = BloomFilter.loadFromBytes(data)

        assertEquals(0, filter.domainCount)
        assertFalse(filter.mightContain("anything.com"))
        assertFalse(filter.mightContain("test.org"))
    }

    @Test
    fun `unicode domains handled correctly`() {
        val domains = setOf("scam-\u00e9xample.com", "\u4e2d\u6587.com")
        val data = buildBloomFilterBytes(domains, bitCount = 1024, hashCount = 7)
        val filter = BloomFilter.loadFromBytes(data)

        for (domain in domains) {
            assertTrue("Unicode domain not found: $domain", filter.mightContain(domain))
        }
    }
}
