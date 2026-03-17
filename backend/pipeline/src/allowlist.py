"""Curated allowlist of legitimate domains (Section 3.3).

Malaysian banks, Singapore banks, Malaysian government, and e-commerce platforms.
"""

from __future__ import annotations

# Malaysian Banks
MALAYSIAN_BANKS: dict[str, list[str]] = {
    "Maybank": ["maybank2u.com.my", "maybank.com.my", "maybank.com"],
    "CIMB": ["cimb.com.my", "cimbclicks.com.my"],
    "Public Bank": ["pbebank.com", "publicbank.com.my"],
    "RHB": ["rhbgroup.com", "rhb.com.my"],
    "Hong Leong": ["hlb.com.my", "hlbb.hongleong.com.my"],
    "Bank Islam": ["bankislam.com.my"],
    "Affin Bank": ["affinbank.com.my", "affinonline.com"],
    "AmBank": ["ambank.com.my", "ambankgroup.com"],
    "BSN": ["mybsn.com.my", "bsn.com.my"],
    "Bank Rakyat": ["bankrakyat.com.my"],
}

# Singapore Banks
SINGAPORE_BANKS: dict[str, list[str]] = {
    "DBS": ["dbs.com.sg", "posb.com.sg"],
    "OCBC": ["ocbc.com"],
    "UOB": ["uob.com.sg"],
    "Standard Chartered": ["sc.com"],
}

# Malaysian Government
MALAYSIAN_GOVERNMENT: dict[str, list[str]] = {
    "LHDN (Tax)": ["hasil.gov.my"],
    "JPJ (Transport)": ["jpj.gov.my"],
    "PDRM (Police)": ["rmp.gov.my"],
    "EPF/KWSP": ["kwsp.gov.my"],
    "MySejahtera": ["mysejahtera.malaysia.gov.my"],
    "MyGovernment": ["malaysia.gov.my"],
}

# E-Commerce
ECOMMERCE: dict[str, list[str]] = {
    "Shopee": ["shopee.com.my", "shopee.sg"],
    "Lazada": ["lazada.com.my", "lazada.sg"],
    "Grab": ["grab.com"],
    "Touch 'n Go": ["tngdigital.com.my"],
}


def get_all_allowlisted_domains() -> list[dict[str, str]]:
    """Return all allowlisted domains with their category and entity name.

    Returns a list of dicts with keys: domain, category, entity.
    """
    results: list[dict[str, str]] = []

    for category_name, category_data in [
        ("malaysian_bank", MALAYSIAN_BANKS),
        ("singapore_bank", SINGAPORE_BANKS),
        ("malaysian_government", MALAYSIAN_GOVERNMENT),
        ("ecommerce", ECOMMERCE),
    ]:
        for entity, domains in category_data.items():
            for domain in domains:
                results.append(
                    {
                        "domain": domain,
                        "category": category_name,
                        "entity": entity,
                    }
                )

    return results


def get_allowlist_domain_set() -> set[str]:
    """Return a flat set of all allowlisted domains."""
    return {entry["domain"] for entry in get_all_allowlisted_domains()}
