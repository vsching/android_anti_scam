"""Bloom filter generation for on-device scam domain lookup.

Uses MurmurHash3 for hashing. Generates a binary file with a header
containing filter parameters so the Android app can deserialize correctly.

Target: ~600KB for 500K domains with <1% false positive rate.
Math: 1.2 bytes/element x 500K = ~600KB for 1% FPR with optimal hash count (k=7).
"""

from __future__ import annotations

import math
import struct
from typing import BinaryIO

import mmh3

# Header format:
# Magic bytes: "SAFB" (4 bytes)
# Version: uint16 (2 bytes)
# Filter size in bits: uint64 (8 bytes)
# Number of hash functions: uint16 (2 bytes)
# Domain count: uint32 (4 bytes)
# Reserved: 12 bytes
# Total header: 32 bytes
HEADER_FORMAT = "<4sHQHI12s"
HEADER_SIZE = struct.calcsize(HEADER_FORMAT)
MAGIC = b"SAFB"
VERSION = 1


def optimal_filter_params(
    n: int, target_fpr: float = 0.01
) -> tuple[int, int]:
    """Calculate optimal Bloom filter parameters.

    Args:
        n: Expected number of elements.
        target_fpr: Target false positive rate (default 1%).

    Returns:
        Tuple of (m: filter size in bits, k: number of hash functions).
    """
    if n <= 0:
        return (64, 1)  # minimum viable filter

    # m = -n * ln(p) / (ln(2))^2
    m = int(-n * math.log(target_fpr) / (math.log(2) ** 2))
    # Round up to multiple of 8 for byte alignment
    m = ((m + 7) // 8) * 8

    # k = (m/n) * ln(2)
    k = max(1, int((m / n) * math.log(2)))

    return (m, k)


def _hash_domain(domain: str, seed: int) -> int:
    """Hash a domain with a given seed using MurmurHash3."""
    return mmh3.hash(domain, seed, signed=False)


class BloomFilter:
    """Bloom filter implementation using MurmurHash3."""

    def __init__(self, size_bits: int, num_hashes: int):
        self.size_bits = size_bits
        self.num_hashes = num_hashes
        self._num_bytes = (size_bits + 7) // 8
        self._bits = bytearray(self._num_bytes)
        self._count = 0

    @classmethod
    def from_domain_count(
        cls, n: int, target_fpr: float = 0.01
    ) -> BloomFilter:
        """Create a BloomFilter sized for n domains with target FPR."""
        m, k = optimal_filter_params(n, target_fpr)
        return cls(m, k)

    def add(self, domain: str) -> None:
        """Add a domain to the filter."""
        for i in range(self.num_hashes):
            h = _hash_domain(domain, seed=i) % self.size_bits
            byte_idx = h // 8
            bit_idx = h % 8
            self._bits[byte_idx] |= 1 << bit_idx
        self._count += 1

    def might_contain(self, domain: str) -> bool:
        """Check if a domain might be in the filter.

        Returns True if domain might be present (possible false positive).
        Returns False if domain is definitely not present (no false negatives).
        """
        for i in range(self.num_hashes):
            h = _hash_domain(domain, seed=i) % self.size_bits
            byte_idx = h // 8
            bit_idx = h % 8
            if not (self._bits[byte_idx] & (1 << bit_idx)):
                return False
        return True

    @property
    def count(self) -> int:
        """Number of elements added."""
        return self._count

    @property
    def size_bytes(self) -> int:
        """Size of the filter bit array in bytes."""
        return self._num_bytes

    def serialize(self) -> bytes:
        """Serialize the filter to bytes with header."""
        header = struct.pack(
            HEADER_FORMAT,
            MAGIC,
            VERSION,
            self.size_bits,
            self.num_hashes,
            self._count,
            b"\x00" * 12,
        )
        return header + bytes(self._bits)

    def write_to_file(self, f: BinaryIO) -> None:
        """Write the serialized filter to a file-like object."""
        f.write(self.serialize())

    @classmethod
    def deserialize(cls, data: bytes) -> BloomFilter:
        """Deserialize a BloomFilter from bytes."""
        if len(data) < HEADER_SIZE:
            raise ValueError("Data too short for Bloom filter header")

        magic, version, size_bits, num_hashes, count, _reserved = (
            struct.unpack(HEADER_FORMAT, data[:HEADER_SIZE])
        )

        if magic != MAGIC:
            raise ValueError(f"Invalid magic bytes: {magic!r}")
        if version != VERSION:
            raise ValueError(f"Unsupported version: {version}")

        bf = cls(size_bits, num_hashes)
        bf._count = count

        expected_bytes = (size_bits + 7) // 8
        filter_data = data[HEADER_SIZE:]
        if len(filter_data) < expected_bytes:
            raise ValueError(
                f"Expected {expected_bytes} filter bytes, got {len(filter_data)}"
            )
        bf._bits = bytearray(filter_data[:expected_bytes])

        return bf


def generate_bloom_filter(
    domains: list[str], target_fpr: float = 0.01
) -> BloomFilter:
    """Generate a Bloom filter from a list of domains.

    Args:
        domains: List of normalized domain strings.
        target_fpr: Target false positive rate (default 1%).

    Returns:
        A populated BloomFilter instance.
    """
    bf = BloomFilter.from_domain_count(len(domains), target_fpr)
    for domain in domains:
        bf.add(domain)
    return bf
