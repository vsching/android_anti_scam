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
    "Bank Rakyat": ["bankrakyat.com.my", "irakyat.com.my"],
    "Alliance Bank": ["alliancebank.com.my", "allianceonline.com.my"],
    "Bank Muamalat": ["muamalat.com.my"],
    "Agrobank": ["agrobank.com.my"],
    "MBSB Bank": ["mbsbbank.com", "mbsb.com.my"],
    "Al Rajhi Bank MY": ["alrajhibank.com.my"],
    "HSBC MY": ["hsbc.com.my"],
    "Citibank MY": ["citibank.com.my"],
    "Standard Chartered MY": ["sc.com.my"],
    "Bank of China MY": ["bankofchina.com.my"],
    "OCBC MY": ["ocbc.com.my"],
    "UOB MY": ["uob.com.my"],
    "CGS-CIMB": ["cgsi.com"],
    "RHB Investment": ["rhbinvest.com.my"],
}

# Singapore Banks
SINGAPORE_BANKS: dict[str, list[str]] = {
    "DBS": ["dbs.com.sg", "dbs.com", "posb.com.sg"],
    "OCBC": ["ocbc.com", "ocbc.com.sg"],
    "UOB": ["uob.com.sg", "uob.com"],
    "Standard Chartered SG": ["sc.com.sg"],
    "HSBC SG": ["hsbc.com.sg"],
    "Citibank SG": ["citibank.com.sg"],
    "Maybank SG": ["maybank.com.sg", "maybank2u.com.sg"],
    "CIMB SG": ["cimb.com.sg"],
    "Bank of China SG": ["bankofchina.com.sg"],
    "ICBC SG": ["icbc.com.sg"],
    "RHB SG": ["rhbbank.com.sg"],
}

# Global Banks (commonly impersonated in MY/SG scams)
GLOBAL_BANKS: dict[str, list[str]] = {
    "HSBC": ["hsbc.com"],
    "Citibank": ["citibank.com", "citi.com"],
    "Standard Chartered": ["sc.com"],
    "Bank of China": ["boc.cn", "bankofchina.com"],
    "ICBC": ["icbc.com.cn"],
    "Deutsche Bank": ["db.com"],
    "JP Morgan": ["jpmorgan.com"],
    "Goldman Sachs": ["goldmansachs.com"],
}

# Malaysian Government
MALAYSIAN_GOVERNMENT: dict[str, list[str]] = {
    "LHDN (Tax)": ["hasil.gov.my"],
    "JPJ (Transport)": ["jpj.gov.my"],
    "PDRM (Police)": ["rmp.gov.my"],
    "EPF/KWSP": ["kwsp.gov.my"],
    "MySejahtera": ["mysejahtera.malaysia.gov.my"],
    "MyGovernment": ["malaysia.gov.my"],
    "BNM": ["bnm.gov.my"],
    "MCMC": ["mcmc.gov.my", "skmm.gov.my"],
    "KPDNHEP": ["kpdnhep.gov.my"],
    "JPN": ["jpn.gov.my"],
    "Immigration": ["imi.gov.my"],
    "SSM": ["ssm.com.my"],
    "Bursa Malaysia": ["bursamalaysia.com"],
    # SC Malaysia (sc.com.my) already listed under Standard Chartered MY in banks
}

# Singapore Government
SINGAPORE_GOVERNMENT: dict[str, list[str]] = {
    "Singpass": ["singpass.gov.sg"],
    "CPF": ["cpf.gov.sg"],
    "IRAS": ["iras.gov.sg"],
    "Gov.sg": ["gov.sg"],
    "MAS": ["mas.gov.sg"],
    "HDB": ["hdb.gov.sg"],
    "MOH": ["moh.gov.sg"],
    "SPF (Police)": ["police.gov.sg"],
    "ICA": ["ica.gov.sg"],
    "MOM": ["mom.gov.sg"],
    "MyInfo": ["myinfo.gov.sg"],
}

# E-Commerce & Digital Payments
ECOMMERCE: dict[str, list[str]] = {
    "Shopee": ["shopee.com.my", "shopee.sg", "shopee.com"],
    "Lazada": ["lazada.com.my", "lazada.sg", "lazada.com"],
    "Grab": ["grab.com"],
    "Touch 'n Go": ["tngdigital.com.my", "touchngo.com.my"],
    "Boost": ["myboost.com.my"],
    "BigPay": ["bigpayme.com"],
    "ShopeePay": ["shopeepay.com.my"],
    "GrabPay": ["grabpay.com"],
    "FavePay": ["myfave.com"],
    "GoPay": ["gopay.com.my"],
    "PayNet": ["paynet.my"],
    "Setel": ["setel.com"],
    "MAE (Maybank)": ["mae.com.my"],
    "Pos Laju": ["pos.com.my"],
    "J&T Express": ["jtexpress.my"],
    "DHL MY": ["dhl.com.my"],
    "Ninja Van": ["ninjavan.co"],

    # SG e-commerce & payments
    "PayNow": ["abs.org.sg"],
    "Qoo10": ["qoo10.sg"],
    "Carousell": ["carousell.sg", "carousell.com"],
    "FairPrice": ["fairprice.com.sg"],
    "SingPost": ["singpost.com"],

    # Global platforms commonly used in MY/SG
    "Apple": ["apple.com"],
    "Google": ["google.com", "google.com.my", "google.com.sg"],
    "WhatsApp": ["whatsapp.com"],
    "Netflix": ["netflix.com"],
    "Facebook": ["facebook.com"],
    "Instagram": ["instagram.com"],
    "TikTok": ["tiktok.com"],

    # Telcos (commonly impersonated)
    "Maxis": ["maxis.com.my"],
    "Celcom": ["celcom.com.my"],
    "Digi": ["digi.com.my"],
    "U Mobile": ["u.com.my"],
    "Unifi": ["unifi.com.my"],
    "Singtel": ["singtel.com"],
    "StarHub": ["starhub.com"],
    "M1": ["m1.com.sg"],
}


def get_all_allowlisted_domains() -> list[dict[str, str]]:
    """Return all allowlisted domains with their category and entity name.

    Returns a list of dicts with keys: domain, category, entity.
    """
    results: list[dict[str, str]] = []

    for category_name, category_data in [
        ("malaysian_bank", MALAYSIAN_BANKS),
        ("singapore_bank", SINGAPORE_BANKS),
        ("global_bank", GLOBAL_BANKS),
        ("malaysian_government", MALAYSIAN_GOVERNMENT),
        ("singapore_government", SINGAPORE_GOVERNMENT),
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
