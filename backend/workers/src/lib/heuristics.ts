// Heuristic engine for scam link detection.
// Checks: typosquatting, suspicious TLDs, bank-name patterns,
// URL shorteners, and subdomain-aware matching.

import { extractRootDomain, getTLD, isPunycode } from './domain';

/** Result of a heuristic check. */
export interface HeuristicResult {
  verdict: 'safe' | 'suspicious' | 'dangerous';
  reason: string;
  confidence: number;
  details: {
    check_type: string;
    similar_to?: string;
    [key: string]: unknown;
  };
}

// --- Allowlisted legitimate domains (Malaysian + Singaporean banks, gov) ---
const LEGITIMATE_DOMAINS: Record<string, string> = {
  'maybank2u.com.my': 'Maybank',
  'maybank.com.my': 'Maybank',
  'maybank.com': 'Maybank',
  'cimbclicks.com.my': 'CIMB',
  'cimb.com.my': 'CIMB',
  'cimb.com': 'CIMB',
  'rhbgroup.com': 'RHB',
  'rhbnow.com': 'RHB',
  'pbebank.com': 'Public Bank',
  'hongleongconnect.my': 'Hong Leong',
  'hlb.com.my': 'Hong Leong',
  'ambankgroup.com': 'AmBank',
  'ambank.com.my': 'AmBank',
  'bankislam.com': 'Bank Islam',
  'bankrakyat.com.my': 'Bank Rakyat',
  'bsn.com.my': 'BSN',
  'dbs.com.sg': 'DBS',
  'dbs.com': 'DBS',
  'ocbc.com': 'OCBC',
  'ocbc.com.sg': 'OCBC',
  'uob.com.sg': 'UOB',
  'uob.com': 'UOB',
  'posb.com.sg': 'POSB',
  'sc.com': 'Standard Chartered',
  'hsbc.com.my': 'HSBC',
  'citibank.com.my': 'Citibank',
  'hasil.gov.my': 'LHDN',
  'lhdn.gov.my': 'LHDN',
  'kwsp.gov.my': 'KWSP/EPF',
  'jpj.gov.my': 'JPJ',
  'iras.gov.sg': 'IRAS',
  'cpf.gov.sg': 'CPF',
  'singpass.gov.sg': 'Singpass',
  'grab.com': 'Grab',
  'shopee.com.my': 'Shopee',
  'shopee.sg': 'Shopee',
  'lazada.com.my': 'Lazada',
  'lazada.sg': 'Lazada',
  'touchngo.com.my': 'Touch n Go',
  'tngdigital.com.my': 'TNG Digital',
};

// Bank/brand keywords for pattern matching
const BANK_KEYWORDS = [
  'maybank', 'cimb', 'rhb', 'publicbank', 'hongleong', 'ambank',
  'bankislam', 'bankrakyat', 'bsn', 'dbs', 'ocbc', 'uob', 'posb',
  'hsbc', 'citibank', 'standardchartered', 'affin', 'alliancebank',
  'lhdn', 'kwsp', 'epf', 'jpj', 'iras', 'cpf', 'singpass',
  'grab', 'shopee', 'lazada', 'touchngo', 'tngdigital', 'maybank2u',
];

// Suspicious TLDs commonly used in scam domains
const SUSPICIOUS_TLDS = new Set([
  'xyz', 'top', 'buzz', 'click', 'loan', 'win',
  'gq', 'ml', 'cf', 'tk', 'ga', 'work', 'fit',
  'icu', 'cam', 'rest', 'monster', 'surf', 'bar',
]);

// URL shortener domains
const URL_SHORTENERS = new Set([
  'bit.ly', 'tinyurl.com', 't.co', 'goo.gl', 'is.gd', 'v.gd',
  'buff.ly', 'ow.ly', 'rebrand.ly', 'bl.ink', 'short.io',
  'cutt.ly', 'rb.gy', 's.id', 'rotf.lol', 'shorturl.at',
  'tiny.cc', 'x.co', 'youtu.be', 'amzn.to',
]);

/**
 * Run all heuristic checks against a domain.
 * Returns the highest-severity result.
 */
export function runHeuristics(domain: string): HeuristicResult {
  const rootDomain = extractRootDomain(domain);

  // 1. Check if it's an exact match for a known legitimate domain
  if (LEGITIMATE_DOMAINS[domain] || LEGITIMATE_DOMAINS[rootDomain]) {
    return {
      verdict: 'safe',
      reason: `Known legitimate domain: ${LEGITIMATE_DOMAINS[domain] || LEGITIMATE_DOMAINS[rootDomain]}`,
      confidence: 1.0,
      details: { check_type: 'allowlist' },
    };
  }

  // Collect all check results and return the most severe
  const results: HeuristicResult[] = [];

  // 2. URL shortener check
  const shortenerResult = checkUrlShortener(domain, rootDomain);
  if (shortenerResult) results.push(shortenerResult);

  // 3. Typosquatting check
  const typoResult = checkTyposquatting(domain, rootDomain);
  if (typoResult) results.push(typoResult);

  // 4. Bank-name pattern check (brand name + random suffix on suspicious TLD)
  const bankPatternResult = checkBankNamePattern(domain, rootDomain);
  if (bankPatternResult) results.push(bankPatternResult);

  // 5. Suspicious TLD check
  const tldResult = checkSuspiciousTLD(domain);
  if (tldResult) results.push(tldResult);

  // 6. Punycode / IDN check
  const punycodeResult = checkPunycode(domain);
  if (punycodeResult) results.push(punycodeResult);

  // 7. Subdomain brand abuse (e.g., maybank.evil.xyz)
  const subdomainResult = checkSubdomainAbuse(domain, rootDomain);
  if (subdomainResult) results.push(subdomainResult);

  // Return the most severe result
  if (results.length === 0) {
    return {
      verdict: 'safe',
      reason: 'No suspicious patterns detected',
      confidence: 0.5,
      details: { check_type: 'none' },
    };
  }

  // Sort by severity: dangerous > suspicious > safe
  const severityOrder: Record<string, number> = { dangerous: 2, suspicious: 1, safe: 0 };
  results.sort((a, b) => severityOrder[b.verdict] - severityOrder[a.verdict]);

  return results[0];
}

/**
 * Compute Levenshtein distance between two strings.
 */
export function levenshteinDistance(a: string, b: string): number {
  const m = a.length;
  const n = b.length;

  // Use single-row optimization
  const prev = new Array(n + 1);
  const curr = new Array(n + 1);

  for (let j = 0; j <= n; j++) prev[j] = j;

  for (let i = 1; i <= m; i++) {
    curr[0] = i;
    for (let j = 1; j <= n; j++) {
      if (a[i - 1] === b[j - 1]) {
        curr[j] = prev[j - 1];
      } else {
        curr[j] = 1 + Math.min(prev[j - 1], prev[j], curr[j - 1]);
      }
    }
    for (let j = 0; j <= n; j++) prev[j] = curr[j];
  }

  return prev[n];
}

function checkUrlShortener(domain: string, rootDomain: string): HeuristicResult | null {
  if (URL_SHORTENERS.has(domain) || URL_SHORTENERS.has(rootDomain)) {
    return {
      verdict: 'suspicious',
      reason: 'URL shortener detected — destination unknown',
      confidence: 0.7,
      details: { check_type: 'url_shortener', shortener: domain },
    };
  }
  return null;
}

function checkTyposquatting(domain: string, rootDomain: string): HeuristicResult | null {
  // Compare root domain (without TLD) against known domains
  const domainBase = rootDomain.split('.')[0];
  let bestMatch: { legit: string; name: string; distance: number } | null = null;

  for (const [legitDomain, name] of Object.entries(LEGITIMATE_DOMAINS)) {
    const legitBase = extractRootDomain(legitDomain).split('.')[0];

    // Skip if same base (exact match would have been caught earlier)
    if (domainBase === legitBase) continue;

    const distance = levenshteinDistance(domainBase, legitBase);
    const maxLen = Math.max(domainBase.length, legitBase.length);

    // Flag if edit distance is 1-2 for short names, 1-3 for longer names
    const threshold = maxLen <= 5 ? 1 : maxLen <= 8 ? 2 : 3;

    if (distance > 0 && distance <= threshold) {
      if (!bestMatch || distance < bestMatch.distance) {
        bestMatch = { legit: legitDomain, name, distance };
      }
    }
  }

  if (bestMatch) {
    const confidence = bestMatch.distance === 1 ? 0.92 : bestMatch.distance === 2 ? 0.8 : 0.65;
    return {
      verdict: 'dangerous',
      reason: `Typosquatting detected: similar to ${bestMatch.legit}`,
      confidence,
      details: {
        check_type: 'typosquatting',
        similar_to: bestMatch.legit,
        edit_distance: bestMatch.distance,
        brand: bestMatch.name,
      },
    };
  }

  return null;
}

function checkBankNamePattern(domain: string, rootDomain: string): HeuristicResult | null {
  const domainBase = rootDomain.split('.')[0].toLowerCase();
  const tld = getTLD(domain);

  for (const keyword of BANK_KEYWORDS) {
    // Check if domain contains bank keyword with extra chars (e.g., maybank-secure, cimb-update)
    if (domainBase !== keyword && domainBase.includes(keyword) && domainBase.length > keyword.length) {
      const isSuspiciousTld = SUSPICIOUS_TLDS.has(tld);
      const severity = isSuspiciousTld ? 'dangerous' : 'suspicious';
      const confidence = isSuspiciousTld ? 0.9 : 0.7;

      return {
        verdict: severity,
        reason: `Brand impersonation: contains "${keyword}" with additional characters`,
        confidence,
        details: {
          check_type: 'bank_pattern',
          keyword,
          tld,
          suspicious_tld: isSuspiciousTld,
        },
      };
    }
  }

  return null;
}

function checkSuspiciousTLD(domain: string): HeuristicResult | null {
  const tld = getTLD(domain);

  if (SUSPICIOUS_TLDS.has(tld)) {
    return {
      verdict: 'suspicious',
      reason: `Suspicious TLD: .${tld} is commonly used in scam domains`,
      confidence: 0.6,
      details: { check_type: 'suspicious_tld', tld },
    };
  }

  return null;
}

function checkPunycode(domain: string): HeuristicResult | null {
  if (isPunycode(domain)) {
    return {
      verdict: 'suspicious',
      reason: 'Internationalized domain name (IDN/Punycode) — may be used for homograph attacks',
      confidence: 0.65,
      details: { check_type: 'punycode', domain },
    };
  }
  return null;
}

function checkSubdomainAbuse(domain: string, rootDomain: string): HeuristicResult | null {
  // If the domain has subdomains, check if any subdomain matches a bank keyword
  if (domain === rootDomain) return null;

  const subdomainPart = domain.slice(0, domain.length - rootDomain.length - 1);
  const subdomains = subdomainPart.split('.');

  for (const sub of subdomains) {
    for (const keyword of BANK_KEYWORDS) {
      if (sub === keyword || sub.includes(keyword)) {
        // Check that the root domain is NOT the legitimate one
        const isLegit = Object.keys(LEGITIMATE_DOMAINS).some(
          d => extractRootDomain(d) === rootDomain,
        );
        if (!isLegit) {
          return {
            verdict: 'dangerous',
            reason: `Subdomain brand abuse: "${sub}" subdomain on unrelated domain ${rootDomain}`,
            confidence: 0.85,
            details: {
              check_type: 'subdomain_abuse',
              subdomain: sub,
              root_domain: rootDomain,
              keyword,
            },
          };
        }
      }
    }
  }

  return null;
}
