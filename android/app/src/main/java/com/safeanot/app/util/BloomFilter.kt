/**
 * Bloom filter implementation using MurmurHash3 for fast probabilistic domain lookups.
 *
 * Binary format (32-byte header):
 * - Bytes 0-3: Magic "SAFB" (0x53, 0x41, 0x46, 0x42)
 * - Bytes 4-7: Version (uint32 LE)
 * - Bytes 8-11: Bit count (uint32 LE)
 * - Bytes 12-15: Hash count (uint32 LE)
 * - Bytes 16-19: Domain count (uint32 LE)
 * - Bytes 20-31: Reserved (zeros)
 * - Remaining bytes: bit array
 */
package com.safeanot.app.util

class BloomFilter private constructor(
    private val bitArray: ByteArray,
    private val bitCount: Int,
    private val hashCount: Int,
    val domainCount: Int,
) {

    fun mightContain(domain: String): Boolean {
        val key = domain.toByteArray(Charsets.UTF_8)
        val hash1 = murmurHash3(key, 0)
        val hash2 = murmurHash3(key, hash1.toInt())
        for (i in 0 until hashCount) {
            val combinedHash = hash1 + i.toLong() * hash2
            val bitIndex = ((combinedHash % bitCount + bitCount) % bitCount).toInt()
            val byteIndex = bitIndex / 8
            val bitOffset = bitIndex % 8
            if (bitArray[byteIndex].toInt() and (1 shl bitOffset) == 0) {
                return false
            }
        }
        return true
    }

    companion object {

        private const val HEADER_SIZE = 32
        private const val MAGIC_S = 0x53.toByte()
        private const val MAGIC_A = 0x41.toByte()
        private const val MAGIC_F = 0x46.toByte()
        private const val MAGIC_B = 0x42.toByte()

        fun loadFromBytes(data: ByteArray): BloomFilter {
            require(data.size >= HEADER_SIZE) { "Data too small: expected at least $HEADER_SIZE bytes, got ${data.size}" }
            require(
                data[0] == MAGIC_S && data[1] == MAGIC_A && data[2] == MAGIC_F && data[3] == MAGIC_B,
            ) { "Invalid magic bytes: expected SAFB" }

            val version = readUint32LE(data, 4)
            require(version == 1L) { "Unsupported version: $version" }

            val bitCount = readUint32LE(data, 8).toInt()
            val hashCount = readUint32LE(data, 12).toInt()
            val domainCount = readUint32LE(data, 16).toInt()

            require(bitCount > 0) { "Bit count must be positive" }
            require(hashCount > 0) { "Hash count must be positive" }

            val expectedBytes = (bitCount + 7) / 8
            require(data.size >= HEADER_SIZE + expectedBytes) {
                "Data too small for bit array: expected ${HEADER_SIZE + expectedBytes} bytes, got ${data.size}"
            }

            val bitArray = data.copyOfRange(HEADER_SIZE, HEADER_SIZE + expectedBytes)
            return BloomFilter(bitArray, bitCount, hashCount, domainCount)
        }

        private fun readUint32LE(data: ByteArray, offset: Int): Long {
            return (data[offset].toLong() and 0xFF) or
                ((data[offset + 1].toLong() and 0xFF) shl 8) or
                ((data[offset + 2].toLong() and 0xFF) shl 16) or
                ((data[offset + 3].toLong() and 0xFF) shl 24)
        }

        /**
         * MurmurHash3 (32-bit, x86) implementation.
         */
        internal fun murmurHash3(key: ByteArray, seed: Int): Long {
            val len = key.size
            var h = seed
            val c1 = -0x3361d2af // 0xcc9e2d51
            val c2 = 0x1b873593

            // Body: process 4-byte blocks
            val nblocks = len / 4
            for (i in 0 until nblocks) {
                val offset = i * 4
                var k = (key[offset].toInt() and 0xFF) or
                    ((key[offset + 1].toInt() and 0xFF) shl 8) or
                    ((key[offset + 2].toInt() and 0xFF) shl 16) or
                    ((key[offset + 3].toInt() and 0xFF) shl 24)

                k = k * c1
                k = Integer.rotateLeft(k, 15)
                k = k * c2

                h = h xor k
                h = Integer.rotateLeft(h, 13)
                h = h * 5 + -0x19ab949c // 0xe6546b64
            }

            // Tail: remaining bytes
            val tailOffset = nblocks * 4
            var k1 = 0
            @Suppress("KotlinConstantConditions")
            when (len and 3) {
                3 -> {
                    k1 = k1 xor ((key[tailOffset + 2].toInt() and 0xFF) shl 16)
                    k1 = k1 xor ((key[tailOffset + 1].toInt() and 0xFF) shl 8)
                    k1 = k1 xor (key[tailOffset].toInt() and 0xFF)
                    k1 = k1 * c1
                    k1 = Integer.rotateLeft(k1, 15)
                    k1 = k1 * c2
                    h = h xor k1
                }
                2 -> {
                    k1 = k1 xor ((key[tailOffset + 1].toInt() and 0xFF) shl 8)
                    k1 = k1 xor (key[tailOffset].toInt() and 0xFF)
                    k1 = k1 * c1
                    k1 = Integer.rotateLeft(k1, 15)
                    k1 = k1 * c2
                    h = h xor k1
                }
                1 -> {
                    k1 = k1 xor (key[tailOffset].toInt() and 0xFF)
                    k1 = k1 * c1
                    k1 = Integer.rotateLeft(k1, 15)
                    k1 = k1 * c2
                    h = h xor k1
                }
            }

            // Finalization mix
            h = h xor len
            h = h xor (h ushr 16)
            h = h * -0x7a143595 // 0x85ebca6b
            h = h xor (h ushr 13)
            h = h * -0x3d4d51cb // 0xc2b2ae35
            h = h xor (h ushr 16)

            return h.toLong() and 0xFFFFFFFFL
        }
    }
}
