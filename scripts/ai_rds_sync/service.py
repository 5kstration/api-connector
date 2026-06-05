import json
import logging
import re
import time

from sqlalchemy.exc import IntegrityError, SQLAlchemyError
from sqlalchemy.orm import Session

from entity import CardProduct, InsuranceProduct, PolicyProduct

logger = logging.getLogger(__name__)

CARD_COMPANY_KEYWORDS = {
    "삼성": "삼성카드",
    "신한": "신한카드",
    "KB국민": "KB국민카드",
    "국민": "KB국민카드",
    "현대": "현대카드",
    "롯데": "롯데카드",
    "우리": "우리카드",
    "하나": "하나카드",
    "농협": "NH농협카드",
    "NH": "NH농협카드",
    "IBK": "IBK기업은행",
    "기업": "IBK기업은행",
    "BC": "BC카드",
}

CATEGORY_COLORS = {
    "일자리": "#3182F6",
    "주거": "#10B981",
    "교육": "#8B5CF6",
    "복지": "#F59E0B",
    "금융": "#06B6D4",
    "문화": "#EC4899",
    "참여": "#6366F1",
    "기타": "#64748B",
}


def create_tsid() -> str:
    millis = int(time.time() * 1000)
    nanos = time.time_ns() % 1_000_000
    return f"{millis:013d}{nanos:06d}"[:26]


def _get(raw: dict, *candidates, default=""):
    for key in candidates:
        val = raw.get(key)
        if val is not None and str(val).strip():
            return val
    return default


def _first_not_empty(*values, default=""):
    for value in values:
        if value is not None and str(value).strip():
            return value
    return default


def _truncate(value, length: int):
    if value is None:
        return None
    value = str(value).strip()
    return value[:length] if value else None


def _parse_date_value(value) -> str:
    if value is None:
        return ""
    text = str(value).strip()
    if not text:
        return ""
    normalized = re.sub(r"[-/.]", "", text)
    if re.fullmatch(r"\d{8}", normalized):
        return f"{normalized[:4]}.{normalized[4:6]}.{normalized[6:]}"
    return text


def _parse_date(raw: dict, *candidates) -> str:
    return _parse_date_value(_get(raw, *candidates))


def _parse_period_end(value) -> str:
    if value is None:
        return ""
    text = str(value).strip()
    dates = re.findall(r"\d{4}[-/.]?\d{2}[-/.]?\d{2}", text)
    if dates:
        return _parse_date_value(dates[-1])
    return text


def _parse_url(raw: dict, *candidates) -> str:
    for key in candidates:
        val = raw.get(key, "")
        if val and re.match(r"https?://", str(val)):
            return val
    return ""


def _parse_int(raw: dict, *candidates):
    val = _get(raw, *candidates, default="")
    if val == "":
        return None
    digits = re.sub(r"[^0-9]", "", str(val))
    return int(digits) if digits else None


def _json_text(value) -> str:
    return json.dumps(value, ensure_ascii=False)


def _benefit_items_from_texts(texts, limit=8) -> str:
    if not isinstance(texts, list):
        return "[]"
    return _json_text([
        {"label": f"혜택 {index + 1}", "value": str(benefit).strip()}
        for index, benefit in enumerate(texts[:limit])
        if benefit is not None and str(benefit).strip()
    ])


def _derive_card_company(raw: dict) -> str:
    explicit = _get(raw, "cardCompany", "company", "issuer", "cmpyNm", "brandNm")
    if explicit:
        return explicit

    section_type = _get(raw, "sourceSectionType")
    section_name = _get(raw, "sourceSectionName")
    if section_type == "CARD_COMPANY" and section_name:
        return section_name

    card_name = _get(raw, "cardName", "name", "productName", "prdNm", "goodsNm")
    for keyword, company in CARD_COMPANY_KEYWORDS.items():
        if keyword in str(card_name):
            return company
    return ""


def _map_card(source_code: str, raw: dict) -> dict:
    benefits = _benefit_items_from_texts(raw.get("benefitTexts") or raw.get("benefits") or [])
    top_benefit = _first_not_empty(
        _get(raw, "summary", "topBenefit", "description", "brief"),
        (raw.get("benefitTexts") or [None])[0] if isinstance(raw.get("benefitTexts"), list) and raw.get("benefitTexts") else None,
    )

    return dict(
        company=_truncate(_derive_card_company(raw), 255),
        card_name=_truncate(_get(raw, "cardName", "name", "productName", "prdNm", "goodsNm"), 255),
        top_benefit=_truncate(top_benefit, 255),
        benefits=benefits,
        apply_url=_parse_url(raw, "detailUrl", "applyUrl", "productUrl", "hpgeUrl", "url"),
        accent_color="#3182F6",
    )


def _map_safe_insurance(raw: dict) -> dict:
    guarantee_items = raw.get("lcgvrGrnt", [])
    benefits = _json_text([
        {"label": item.get("grntItmNm", ""), "value": item.get("grntCnts", "")}
        for item in guarantee_items[:5]
        if isinstance(item, dict)
    ])
    top_benefit = _first_not_empty(
        _get(raw, "insrdNm", "mog", "summary"),
        guarantee_items[0].get("grntItmNm") if guarantee_items and isinstance(guarantee_items[0], dict) else None,
    )
    return dict(
        insurer=_truncate(_get(raw, "orgNm", "upOrgNm", "ctrtrNm"), 255),
        insurance_name=_truncate(_get(raw, "insrncGdsNm", "prdNm", "productName"), 255),
        top_benefit=_truncate(top_benefit, 255),
        benefits=benefits,
        apply_url=_parse_url(raw, "hpgeUrl", "applyUrl", "url"),
        accent_color="#8B5CF6",
    )


def _map_indemnity_insurance(raw: dict) -> dict:
    benefits = _json_text([
        {"label": "보험 유형", "value": _get(raw, "ptrn")},
        {"label": "담보", "value": _get(raw, "mog")},
        {"label": "연령", "value": _get(raw, "age")},
        {"label": "남성 보험료", "value": _get(raw, "mlInsRt")},
        {"label": "여성 보험료", "value": _get(raw, "fmlInsRt")},
        {"label": "기준일", "value": _parse_date(raw, "basDt")},
    ])
    return dict(
        insurer=_truncate(_get(raw, "cmpyNm", "companyName", "issuer", "orgNm"), 255),
        insurance_name=_truncate(_get(raw, "prdNm", "productName", "insrncGdsNm", "goodsNm"), 255),
        top_benefit=_truncate(_get(raw, "mog", "ptrn", "summary", "topBenefit"), 255),
        benefits=benefits,
        apply_url=_parse_url(raw, "hpgeUrl", "applyUrl", "url"),
        accent_color="#8B5CF6",
    )


def _map_post_insurance_best(raw: dict) -> dict:
    age_rates = [
        ("10대", raw.get("AGRP_10_RATE")),
        ("20대", raw.get("AGRP_20_RATE")),
        ("30대", raw.get("AGRP_30_RATE")),
        ("40대", raw.get("AGRP_40_RATE")),
        ("50대", raw.get("AGRP_50_RATE")),
        ("60대", raw.get("AGRP_60_RATE")),
        ("70대", raw.get("AGRP_70_RATE")),
    ]
    benefits = _json_text([
        {"label": label, "value": f"{value}%"}
        for label, value in age_rates
        if value is not None and str(value).strip() != ""
    ])
    top_benefit = _first_not_empty(
        f"20대 {raw.get('AGRP_20_RATE')}%, 30대 {raw.get('AGRP_30_RATE')}% 가입 비율",
        _get(raw, "CRTR_YM"),
    )
    return dict(
        insurer="우체국",
        insurance_name=_truncate(_get(raw, "INSU_GDS_NM"), 255),
        top_benefit=_truncate(top_benefit, 255),
        benefits=benefits,
        apply_url="",
        accent_color="#8B5CF6",
    )


def _map_post_insurance_product(raw: dict) -> dict:
    min_period = _get(raw, "PINS_DVSN_MIN_VAL")
    max_period = _get(raw, "PINS_DVSN_GRST_VAL")
    period_unit = _get(raw, "UNIT_CLUS_NM")
    benefits = _json_text([
        {"label": "가입 대상", "value": _get(raw, "PPSN_ASCT_DVSN_CD_NM")},
        {"label": "금리 연동", "value": _get(raw, "RINT_LNKG_DVSN_CD_NM")},
        {"label": "선납 할인", "value": _get(raw, "PPAM_DSCNT_YN")},
        {"label": "보험 기간", "value": f"{min_period}~{max_period}{period_unit}".strip("~")},
        {"label": "온라인 보험", "value": _get(raw, "ONLN_INSU_YN_S1")},
    ])
    return dict(
        insurer="우체국",
        insurance_name=_truncate(_get(raw, "GDS_NM"), 255),
        top_benefit=_truncate(_get(raw, "GDS_CLSF_NM", default="우체국 보험상품"), 255),
        benefits=benefits,
        apply_url="",
        accent_color="#8B5CF6",
    )


def _map_post_insurance_coverage(raw: dict) -> dict:
    benefits = _json_text([
        {"label": _get(raw, "GIVE_BNFI_NM", default="보장 내용"), "value": _get(raw, "GIVE_CN_SNTC_CN")},
        {"label": "적용 시작일", "value": _parse_date(raw, "APLCN_BGNG_YMD")},
        {"label": "적용 종료일", "value": _parse_date(raw, "APLCN_END_YMD")},
    ])
    return dict(
        insurer="우체국",
        insurance_name=_truncate(_get(raw, "INSU_NM"), 255),
        top_benefit=_truncate(_get(raw, "GIVE_BNFI_NM", "GIVE_CN_SNTC_CN"), 255),
        benefits=benefits,
        apply_url="",
        accent_color="#8B5CF6",
    )


def _map_insurance(source_code: str, raw: dict) -> dict:
    if source_code == "SAFE_INSURANCE":
        return _map_safe_insurance(raw)
    if source_code == "INDEMNITY_INSURANCE":
        return _map_indemnity_insurance(raw)
    if source_code == "POST_INSURANCE_BEST":
        return _map_post_insurance_best(raw)
    if source_code == "POST_INSURANCE_PRODUCT":
        return _map_post_insurance_product(raw)
    if source_code == "POST_INSURANCE_COVERAGE":
        return _map_post_insurance_coverage(raw)

    return dict(
        insurer=_truncate(_get(raw, "cmpyNm", "companyName", "orgNm", "upOrgNm", "issuer"), 255),
        insurance_name=_truncate(_get(raw, "prdNm", "insrncGdsNm", "productName", "goodsNm", "insrncPrdNm", "GDS_NM", "INSU_NM", "INSU_GDS_NM"), 255),
        top_benefit=_truncate(_get(raw, "mog", "insrdNm", "summary", "topBenefit", "gdsClsNm", "GDS_CLSF_NM", "GIVE_BNFI_NM"), 255),
        benefits="[]",
        apply_url=_parse_url(raw, "hpgeUrl", "applyUrl", "url", "termsUrl"),
        accent_color="#8B5CF6",
    )


def _policy_tags(raw: dict) -> str:
    keyword_raw = _get(raw, "plcyKywdNm", "keywords", "keyword", "tags")
    if isinstance(keyword_raw, list):
        values = keyword_raw[:5]
    else:
        values = [keyword.strip() for keyword in str(keyword_raw).split(",")[:5] if keyword.strip()] if keyword_raw else []

    middle_category = _get(raw, "mclsfNm")
    if middle_category and middle_category not in values:
        values.append(middle_category)
    return _json_text(values[:5])


def _policy_category_color(category: str) -> str:
    for keyword, color in CATEGORY_COLORS.items():
        if keyword in str(category):
            return color
    return CATEGORY_COLORS["기타"]


def _policy_core_benefit(raw: dict) -> str:
    return _truncate(_first_not_empty(
        _get(raw, "plcySprtCn"),
        _get(raw, "plcyExplnCn"),
        _get(raw, "coreBenefit"),
        _get(raw, "summary"),
    ), 255)


def _policy_description(raw: dict) -> str:
    parts = []
    for label, value in [
        ("정책 설명", _get(raw, "plcyExplnCn")),
        ("지원 내용", _get(raw, "plcySprtCn")),
        ("신청 방법", _get(raw, "plcyAplyMthdCn")),
        ("심사 방법", _get(raw, "srngMthdCn")),
        ("제출 서류", _get(raw, "sbmsnDcmntCn")),
        ("기타 사항", _get(raw, "etcMttrCn")),
    ]:
        if value:
            parts.append(f"[{label}] {value}")
    return "\n".join(parts) if parts else _get(raw, "description", "applyMethod", "aplcMthd", "clmMthd")


def _map_policy(source_code: str, raw: dict) -> dict:
    category = _get(raw, "lclsfNm", "bizTycdNm", "polyBizSecd", "category", "policyCategory") or "기타"
    period = _get(raw, "aplyYmd", "applicationPeriod")
    deadline = _first_not_empty(
        _parse_period_end(period),
        _parse_date(raw, "grntEnd", "endDate", "plcyApplyEndDt", "aplcEndDt", "bizPrdEndYmd"),
    )

    start = _first_not_empty(
        _parse_date(raw, "bizPrdBgngYmd", "grntFrom", "startDate", "plcyApplyStrtDt", "aplcStrtDt"),
        "",
    )
    end = _first_not_empty(
        _parse_date(raw, "bizPrdEndYmd", "grntEnd", "endDate", "plcyApplyEndDt", "aplcEndDt"),
        "",
    )
    application_period = period or (f"{start} ~ {end}" if start or end else "")

    income = _first_not_empty(
        _get(raw, "earnEtcCn"),
        f"{_get(raw, 'earnMinAmt')} ~ {_get(raw, 'earnMaxAmt')}" if _get(raw, "earnMinAmt") or _get(raw, "earnMaxAmt") else "",
        _get(raw, "incomeCondition", "income"),
    )

    return dict(
        policy_name=_truncate(_get(raw, "plcyNm", "policyName", "polyBizNm", "name", "insrncGdsNm"), 255),
        org=_truncate(_get(raw, "sprvsnInstCdNm", "operInstCdNm", "rgtrInstCdNm", "orgNm", "upOrgNm", "organNm", "jrsdInsttNm", "institution"), 255),
        category=_truncate(category, 100),
        category_color=_policy_category_color(category),
        deadline=_truncate(deadline, 50),
        dday=None,
        tags=_policy_tags(raw),
        core_benefit=_policy_core_benefit(raw),
        description=_policy_description(raw),
        age_min=_parse_int(raw, "sprtTrgtMinAge", "ageMin", "minAge"),
        age_max=_parse_int(raw, "sprtTrgtMaxAge", "ageMax", "maxAge"),
        income_condition=_truncate(income, 255),
        employment_condition=_truncate(_get(raw, "jobCd", "employmentCondition", "employment"), 255),
        education_condition=_truncate(_get(raw, "schoolCd", "educationCondition", "education"), 255),
        application_period=_truncate(application_period, 255),
        apply_url=_parse_url(raw, "aplyUrlAddr", "refUrlAddr1", "hpgeUrl", "applyUrl", "plcyHomeUrl", "url"),
    )


class SyncService:
    def __init__(self, db: Session):
        self.db = db

    def sync_products(self, items: list[dict], category: str) -> dict:
        saved, updated, skipped, failed = 0, 0, 0, 0

        for item in items:
            try:
                raw = item.get("rawPayload", {}) or {}
                external_id = item.get("externalId")
                source_code = item.get("sourceCode", "")

                if not external_id:
                    logger.warning("[SyncService] externalId missing. sourceCode=%s", source_code)
                    failed += 1
                    continue

                if category == "CARD":
                    model = CardProduct
                    mapped = _map_card(source_code, raw)
                elif category == "INSURANCE":
                    model = InsuranceProduct
                    mapped = _map_insurance(source_code, raw)
                elif category == "POLICY":
                    model = PolicyProduct
                    mapped = _map_policy(source_code, raw)
                    mapped.setdefault("conflict_policy_ids", "[]")
                else:
                    skipped += 1
                    continue

                existing = self.db.query(model).filter(model.external_id == external_id).first()
                if existing:
                    for key, value in mapped.items():
                        setattr(existing, key, value)
                    updated += 1
                else:
                    self.db.add(model(key=create_tsid(), external_id=external_id, **mapped))
                    saved += 1

            except IntegrityError:
                self.db.rollback()
                skipped += 1
                logger.warning("[SyncService] duplicated external_id skipped. external_id=%s", item.get("externalId"))
            except (TypeError, ValueError, AttributeError) as exc:
                failed += 1
                logger.exception("[SyncService] payload mapping failed. external_id=%s error=%s", item.get("externalId"), exc)
            except SQLAlchemyError as exc:
                self.db.rollback()
                failed += 1
                logger.exception("[SyncService] database save failed. external_id=%s error=%s", item.get("externalId"), exc)

        self._commit(category)
        logger.info("[SyncService] %s sync completed. saved=%s updated=%s skipped=%s failed=%s", category, saved, updated, skipped, failed)
        return {"saved": saved, "updated": updated, "skipped": skipped, "failed": failed}

    def _commit(self, label: str) -> None:
        try:
            self.db.commit()
        except SQLAlchemyError as exc:
            self.db.rollback()
            logger.exception("[SyncService] %s commit failed. error=%s", label, exc)
            raise
