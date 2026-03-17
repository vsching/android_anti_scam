"""Tests for Bloom filter generation and correctness."""

import random
import string
import struct

import pytest

from src.bloom_filter import (
    HEADER_FORMAT,
    HEADER_SIZE,
    MAGIC,
    VERSION,
    BloomFilter,
    generate_bloom_filter,
    optimal_filter_params,
)


class TestOptimalFilterParams:
    def test_500k_domains(self):
        m, k = optimal_filter_params(500_000, 0.01)
        # m should be ~4.8M bits (~600KB)
        assert m > 4_000_000
        assert m < 6_000_000
        # k should be ~7
        assert 5 <= k <= 10

    def test_small_count(self):
        m, k = optimal_filter_params(100, 0.01)
        assert m > 0
        assert k >= 1

    def test_zero_count(self):
        m, k = optimal_filter_params(0)
        assert m >= 64
        assert k >= 1

    def test_byte_aligned(self):
        m, k = optimal_filter_params(1000)
        assert m % 8 == 0


class TestBloomFilter:
    def test_add_and_check(self):
        bf = BloomFilter.from_domain_count(100)
        bf.add("example.com")
        assert bf.might_contain("example.com") is True

    def test_no_false_negatives(self):
        domains = [f"domain{i}.com" for i in range(1000)]
        bf = BloomFilter.from_domain_count(len(domains))
        for d in domains:
            bf.add(d)
        for d in domains:
            assert bf.might_contain(d) is True, f"False negative for {d}"

    def test_false_positive_rate(self):
        """Test that FPR is below 1% for a properly-sized filter."""
        n = 10_000
        bf = BloomFilter.from_domain_count(n, target_fpr=0.01)

        # Insert n domains
        inserted = [f"inserted-{i}.example.com" for i in range(n)]
        for d in inserted:
            bf.add(d)

        # Test against domains NOT inserted
        test_count = 10_000
        false_positives = 0
        for i in range(test_count):
            test_domain = f"notinserted-{i}-{random.randint(0, 999999)}.test.org"
            if bf.might_contain(test_domain):
                false_positives += 1

        fpr = false_positives / test_count
        assert fpr < 0.02, f"FPR too high: {fpr:.4f} (expected <0.02)"

    def test_count(self):
        bf = BloomFilter.from_domain_count(100)
        assert bf.count == 0
        bf.add("a.com")
        assert bf.count == 1
        bf.add("b.com")
        assert bf.count == 2

    def test_size_for_500k(self):
        bf = BloomFilter.from_domain_count(500_000, target_fpr=0.01)
        # Should be approximately 600KB
        size_kb = bf.size_bytes / 1024
        assert 400 < size_kb < 800, f"Size {size_kb:.0f}KB out of range"

    def test_serialize_deserialize(self):
        bf = BloomFilter.from_domain_count(100)
        domains = ["a.com", "b.com", "c.com"]
        for d in domains:
            bf.add(d)

        data = bf.serialize()
        bf2 = BloomFilter.deserialize(data)

        assert bf2.size_bits == bf.size_bits
        assert bf2.num_hashes == bf.num_hashes
        assert bf2.count == bf.count
        for d in domains:
            assert bf2.might_contain(d) is True

    def test_header_format(self):
        bf = BloomFilter.from_domain_count(100)
        bf.add("test.com")
        data = bf.serialize()

        magic, version, size_bits, num_hashes, count, _ = struct.unpack(
            HEADER_FORMAT, data[:HEADER_SIZE]
        )
        assert magic == MAGIC
        assert version == VERSION
        assert size_bits == bf.size_bits
        assert num_hashes == bf.num_hashes
        assert count == 1

    def test_deserialize_invalid_magic(self):
        data = b"XXXX" + b"\x00" * 100
        with pytest.raises(ValueError, match="Invalid magic"):
            BloomFilter.deserialize(data)

    def test_deserialize_too_short(self):
        with pytest.raises(ValueError, match="too short"):
            BloomFilter.deserialize(b"short")


class TestGenerateBloomFilter:
    def test_generates_filter(self):
        domains = ["a.com", "b.com", "c.com"]
        bf = generate_bloom_filter(domains)
        assert bf.count == 3
        for d in domains:
            assert bf.might_contain(d) is True

    def test_empty_list(self):
        bf = generate_bloom_filter([])
        assert bf.count == 0
