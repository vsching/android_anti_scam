// Per-domain in-memory miss coalescing.
// Ensures that concurrent requests for the same domain share a single
// heuristic evaluation rather than duplicating work.

import { HeuristicResult } from './heuristics';

/** In-flight promise map: domain -> pending check result. */
const inflightMap = new Map<string, Promise<HeuristicResult>>();

/**
 * Run a check function with in-flight coalescing.
 * If a check for the same domain is already in progress, the existing
 * promise is returned instead of starting a new one.
 */
export async function coalesce(
  domain: string,
  fn: () => Promise<HeuristicResult>,
): Promise<HeuristicResult> {
  const existing = inflightMap.get(domain);
  if (existing) {
    return existing;
  }

  const promise = fn().finally(() => {
    inflightMap.delete(domain);
  });

  inflightMap.set(domain, promise);
  return promise;
}

/**
 * Clear all in-flight entries (useful for testing).
 */
export function clearInflight(): void {
  inflightMap.clear();
}
