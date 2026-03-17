// URL/domain parsing helpers for the link checker.
// Strips protocol, path, query params, fragments, and www prefix.
// Handles Punycode/IDN domains (xn-- encoded).

/**
 * Extract a clean domain from a URL string or bare domain.
 * Strips protocol, path, query, fragment, auth, port, and www prefix.
 */
export function extractDomain(input: string): string | null {
  const trimmed = input.trim();
  if (!trimmed) return null;

  let hostname: string;

  // If it looks like a URL (has ://), parse with URL constructor
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(trimmed)) {
    try {
      const url = new URL(trimmed);
      hostname = url.hostname;
    } catch {
      return null;
    }
  } else {
    // Try adding https:// to parse as URL (handles domain:port, domain/path, etc.)
    try {
      const url = new URL(`https://${trimmed}`);
      hostname = url.hostname;
    } catch {
      return null;
    }
  }

  // Remove trailing dot (FQDN notation)
  hostname = hostname.replace(/\.$/, '');

  // Strip www prefix
  hostname = stripWww(hostname);

  // Lowercase (URL constructor already does this, but be safe)
  hostname = hostname.toLowerCase();

  // Basic validation: must have at least one dot and only valid chars
  if (!hostname.includes('.')) return null;
  if (!/^[a-z0-9.-]+$/.test(hostname)) return null;

  return hostname;
}

/**
 * Strip the www. prefix from a hostname.
 */
export function stripWww(hostname: string): string {
  return hostname.replace(/^www\./, '');
}

/**
 * Extract the root domain (registrable domain) from a hostname.
 * Simple heuristic: returns last two segments, or last three for known
 * two-part TLDs (co.uk, com.my, com.sg, com.au, etc.)
 */
export function extractRootDomain(hostname: string): string {
  const parts = hostname.split('.');
  if (parts.length <= 2) return hostname;

  // Known two-part TLDs common in MY/SG context
  const twoPartTLDs = new Set([
    'com.my', 'org.my', 'net.my', 'edu.my', 'gov.my',
    'com.sg', 'org.sg', 'net.sg', 'edu.sg', 'gov.sg',
    'co.uk', 'org.uk', 'ac.uk',
    'com.au', 'org.au', 'net.au',
    'co.jp', 'co.nz', 'co.id', 'co.th',
    'com.ph', 'com.vn', 'com.tw', 'com.hk', 'com.cn',
    'com.br', 'com.mx',
  ]);

  const lastTwo = parts.slice(-2).join('.');
  if (twoPartTLDs.has(lastTwo)) {
    return parts.slice(-3).join('.');
  }

  return parts.slice(-2).join('.');
}

/**
 * Check if a domain is Punycode-encoded (IDN domain).
 */
export function isPunycode(domain: string): boolean {
  return domain.split('.').some(label => label.startsWith('xn--'));
}

/**
 * Get the TLD from a domain.
 */
export function getTLD(domain: string): string {
  const parts = domain.split('.');
  return parts[parts.length - 1];
}
