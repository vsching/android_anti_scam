// Heuristic engine for scam link detection.
// Checks: typosquatting, suspicious TLDs, bank-name patterns,
// URL shorteners, and subdomain-aware matching.

import { extractRootDomain, getTLD, isPunycode } from './domain';

/** Result of a heuristic check. */
export interface HeuristicResult {
  verdict: 'safe' | 'suspicious' | 'dangerous' | 'unknown';
  reason: string;
  confidence: number;
  details: {
    check_type: string;
    similar_to?: string;
    [key: string]: unknown;
  };
}

// --- Allowlisted legitimate domains (synced with pipeline allowlist.py) ---
const LEGITIMATE_DOMAINS: Record<string, string> = {
  // Malaysian banks
  'maybank2u.com.my': 'Maybank', 'maybank.com.my': 'Maybank', 'maybank.com': 'Maybank',
  'cimb.com.my': 'CIMB', 'cimbclicks.com.my': 'CIMB',
  'pbebank.com': 'Public Bank', 'publicbank.com.my': 'Public Bank',
  'rhbgroup.com': 'RHB', 'rhb.com.my': 'RHB', 'rhbinvest.com.my': 'RHB Investment',
  'hlb.com.my': 'Hong Leong', 'hlbb.hongleong.com.my': 'Hong Leong',
  'bankislam.com.my': 'Bank Islam',
  'affinbank.com.my': 'Affin Bank', 'affinonline.com': 'Affin Bank',
  'ambank.com.my': 'AmBank', 'ambankgroup.com': 'AmBank',
  'mybsn.com.my': 'BSN', 'bsn.com.my': 'BSN',
  'bankrakyat.com.my': 'Bank Rakyat', 'irakyat.com.my': 'Bank Rakyat',
  'alliancebank.com.my': 'Alliance Bank', 'allianceonline.com.my': 'Alliance Bank',
  'muamalat.com.my': 'Bank Muamalat', 'agrobank.com.my': 'Agrobank',
  'mbsbbank.com': 'MBSB Bank', 'mbsb.com.my': 'MBSB Bank',
  'alrajhibank.com.my': 'Al Rajhi Bank', 'cgsi.com': 'CGS-CIMB',
  'hsbc.com.my': 'HSBC MY', 'citibank.com.my': 'Citibank MY',
  'sc.com.my': 'Standard Chartered MY',
  'bankofchina.com.my': 'Bank of China MY',
  'ocbc.com.my': 'OCBC MY', 'uob.com.my': 'UOB MY',
  // Singapore banks
  'dbs.com.sg': 'DBS', 'dbs.com': 'DBS', 'posb.com.sg': 'POSB',
  'ocbc.com': 'OCBC', 'ocbc.com.sg': 'OCBC',
  'uob.com.sg': 'UOB', 'uob.com': 'UOB',
  'sc.com.sg': 'Standard Chartered SG', 'hsbc.com.sg': 'HSBC SG',
  'citibank.com.sg': 'Citibank SG',
  'maybank.com.sg': 'Maybank SG', 'maybank2u.com.sg': 'Maybank SG',
  'cimb.com.sg': 'CIMB SG', 'bankofchina.com.sg': 'Bank of China SG',
  'icbc.com.sg': 'ICBC SG', 'rhbbank.com.sg': 'RHB SG',
  // Global banks
  'hsbc.com': 'HSBC', 'citibank.com': 'Citibank', 'citi.com': 'Citibank',
  'sc.com': 'Standard Chartered', 'boc.cn': 'Bank of China',
  'bankofchina.com': 'Bank of China', 'icbc.com.cn': 'ICBC',
  'db.com': 'Deutsche Bank', 'jpmorgan.com': 'JP Morgan',
  'goldmansachs.com': 'Goldman Sachs',
  // MY government
  'hasil.gov.my': 'LHDN', 'jpj.gov.my': 'JPJ', 'rmp.gov.my': 'PDRM',
  'kwsp.gov.my': 'KWSP/EPF', 'mysejahtera.malaysia.gov.my': 'MySejahtera',
  'malaysia.gov.my': 'MyGovernment', 'bnm.gov.my': 'BNM',
  'mcmc.gov.my': 'MCMC', 'skmm.gov.my': 'MCMC',
  'kpdnhep.gov.my': 'KPDNHEP', 'jpn.gov.my': 'JPN', 'imi.gov.my': 'Immigration',
  'ssm.com.my': 'SSM', 'bursamalaysia.com': 'Bursa Malaysia',
  // SG government
  'singpass.gov.sg': 'Singpass', 'cpf.gov.sg': 'CPF', 'iras.gov.sg': 'IRAS',
  'gov.sg': 'Gov.sg', 'mas.gov.sg': 'MAS', 'hdb.gov.sg': 'HDB',
  'moh.gov.sg': 'MOH', 'police.gov.sg': 'SPF', 'ica.gov.sg': 'ICA',
  'mom.gov.sg': 'MOM', 'myinfo.gov.sg': 'MyInfo',
  // E-commerce & payments
  'shopee.com.my': 'Shopee', 'shopee.sg': 'Shopee', 'shopee.com': 'Shopee',
  'lazada.com.my': 'Lazada', 'lazada.sg': 'Lazada', 'lazada.com': 'Lazada',
  'grab.com': 'Grab', 'tngdigital.com.my': 'TNG Digital', 'touchngo.com.my': 'Touch n Go',
  'myboost.com.my': 'Boost', 'bigpayme.com': 'BigPay',
  'shopeepay.com.my': 'ShopeePay', 'grabpay.com': 'GrabPay',
  'myfave.com': 'FavePay', 'gopay.com.my': 'GoPay',
  'paynet.my': 'PayNet', 'setel.com': 'Setel', 'mae.com.my': 'MAE',
  'pos.com.my': 'Pos Laju', 'jtexpress.my': 'J&T Express',
  'dhl.com.my': 'DHL MY', 'ninjavan.co': 'Ninja Van',
  'abs.org.sg': 'PayNow', 'qoo10.sg': 'Qoo10',
  'carousell.sg': 'Carousell', 'carousell.com': 'Carousell',
  'fairprice.com.sg': 'FairPrice', 'singpost.com': 'SingPost',
  // Global platforms
  'apple.com': 'Apple', 'google.com': 'Google',
  'google.com.my': 'Google MY', 'google.com.sg': 'Google SG',
  'whatsapp.com': 'WhatsApp', 'netflix.com': 'Netflix',
  'facebook.com': 'Facebook', 'instagram.com': 'Instagram', 'tiktok.com': 'TikTok',
  // Telcos
  'maxis.com.my': 'Maxis', 'celcom.com.my': 'Celcom', 'digi.com.my': 'Digi',
  'u.com.my': 'U Mobile', 'unifi.com.my': 'Unifi',
  'singtel.com': 'Singtel', 'starhub.com': 'StarHub', 'm1.com.sg': 'M1',
};

// Bank/brand keywords for pattern matching
const BANK_KEYWORDS = [
  // Malaysian banks
  'maybank', 'maybank2u', 'cimb', 'cimbclicks', 'rhb', 'publicbank', 'pbebank',
  'hongleong', 'ambank', 'bankislam', 'bankrakyat', 'bsn', 'affin',
  'alliancebank', 'muamalat', 'agrobank', 'mbsb', 'alrajhi',
  // Singapore banks
  'dbs', 'posb', 'ocbc', 'uob',
  // Global banks
  'hsbc', 'citibank', 'standardchartered', 'bankofchina', 'icbc',
  // MY government
  'lhdn', 'hasil', 'kwsp', 'epf', 'jpj', 'bnm', 'mcmc', 'kpdnhep', 'ssm',
  // SG government
  'singpass', 'cpf', 'iras', 'myinfo',
  // E-commerce & payments
  'grab', 'grabpay', 'shopee', 'shopeepay', 'lazada', 'touchngo', 'tngdigital',
  'boost', 'bigpay', 'favepay', 'setel', 'mae', 'paynet',
  // Telcos
  'maxis', 'celcom', 'digi', 'umobile', 'unifi', 'singtel', 'starhub',
  // Delivery (commonly spoofed)
  'poslaju', 'jtexpress', 'ninjavan',
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

  // 8. Country-suffix brand impersonation (e.g., applemy.com, grabsg.com)
  const countrySuffixResult = checkCountrySuffixPattern(domain, rootDomain);
  if (countrySuffixResult) results.push(countrySuffixResult);

  // Return the most severe result
  if (results.length === 0) {
    return {
      verdict: 'unknown',
      reason: 'Domain not in our database — proceed with caution',
      confidence: 0.0,
      details: { check_type: 'none' },
    };
  }

  // Sort by severity: dangerous > suspicious > unknown > safe
  const severityOrder: Record<string, number> = { dangerous: 3, suspicious: 2, unknown: 1, safe: 0 };
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

// Country codes used in brand impersonation (e.g., applemy.com, grabsg.com)
const COUNTRY_SUFFIXES = new Set([
  'my', 'sg', 'ph', 'id', 'th', 'vn', 'hk', 'tw', 'jp', 'kr', 'in',
  'uk', 'us', 'au', 'nz', 'asia',
]);

// Brand names for country-suffix detection (legitimate brands never use brandcountry.com)
const BRAND_NAMES = [
  'apple', 'google', 'netflix', 'amazon', 'microsoft', 'whatsapp',
  'facebook', 'instagram', 'tiktok', 'telegram', 'paypal', 'youtube',
  'twitter', 'linkedin', 'spotify', 'uber', 'airbnb',
  'maybank', 'cimb', 'rhb', 'ocbc', 'uob', 'dbs', 'hsbc', 'citibank',
  'grab', 'shopee', 'lazada', 'boost', 'bigpay', 'setel',
  'maxis', 'celcom', 'digi', 'unifi', 'singtel', 'starhub',
  'singpass', 'poslaju', 'ninjavan',
];

function checkCountrySuffixPattern(_domain: string, rootDomain: string): HeuristicResult | null {
  const domainBase = rootDomain.split('.')[0].toLowerCase();

  for (const brand of BRAND_NAMES) {
    for (const country of COUNTRY_SUFFIXES) {
      // Match patterns like: applemy, applemalaysia, grabsg, grabsingapore
      const patterns = [brand + country];
      if (country === 'my') patterns.push(brand + 'malaysia');
      if (country === 'sg') patterns.push(brand + 'singapore');

      for (const pattern of patterns) {
        if (domainBase === pattern) {
          return {
            verdict: 'suspicious',
            reason: `Likely brand impersonation: "${brand}" + country code "${country}". Real ${brand} uses ${brand}.com or ${brand}.com.${country}`,
            confidence: 0.85,
            details: {
              check_type: 'country_suffix',
              brand,
              country_code: country,
              expected_domain: `${brand}.com.${country}`,
            },
          };
        }
      }
    }
  }

  return null;
}
